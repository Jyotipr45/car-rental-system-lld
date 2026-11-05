package com.jyoti.carrental;

import com.jyoti.carrental.config.CarRentalSystem;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.CreateBookingRequest;
import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.dto.request.LoginRequest;
import com.jyoti.carrental.dto.request.PaymentRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.dto.response.BookingResponse;
import com.jyoti.carrental.logging.LogSanitizer;
import com.jyoti.carrental.model.enums.CarType;
import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.TransmissionType;
import com.jyoti.carrental.model.enums.UserRole;
import com.jyoti.carrental.support.LogCapture;
import com.jyoti.carrental.support.MiniTest;
import com.jyoti.carrental.support.MutableClock;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.jyoti.carrental.support.MiniTest.assertEquals;
import static com.jyoti.carrental.support.MiniTest.assertTrue;

/**
 * Proves the audit trail and the debug trace are complete, correlated and safe.
 *
 * @author Jyoti Prakash Parida
 */
public class LoggingAndAuditTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private final LogCapture logs = LogCapture.install();
    private final MutableClock clock = new MutableClock(TODAY.atTime(10, 0).atZone(IST).toInstant(), IST);
    private final CarRentalSystem system = CarRentalSystem.create(clock, false);

    private final Long adminId;
    private final Long customerId;
    private final Long otherId;
    private final Long carId;

    public LoggingAndAuditTest() {
        adminId = register("admin@test.com", UserRole.ADMIN);
        customerId = register("riya.sharma@test.com", UserRole.CUSTOMER);
        otherId = register("arjun@test.com", UserRole.CUSTOMER);
        Long branchId = system.branchService().createBranch(adminId, new CreateBranchRequest("Baner", "Baner Rd", "Pune")).getId();
        carId = system.vehicleService().addCar(adminId, new AddCarRequest("MH12AA0001", "Maruti", "Swift",
                FuelType.PETROL, new BigDecimal("2000"), branchId, CarType.HATCHBACK, TransmissionType.MANUAL, 5)).getId();
    }

    public static void main(String[] args) throws Exception {
        MiniTest.run(LoggingAndAuditTest.class);
    }

    public void testFullBookingLifecycleIsAudited() {
        var bookings = system.bookingService();
        Long id = bookings.createBooking(new CreateBookingRequest(customerId, carId, TODAY, TODAY.plusDays(1))).getId();
        bookings.payForBooking(new PaymentRequest(id, customerId, PaymentMethod.UPI));
        bookings.markPickup(adminId, id);
        bookings.markReturn(adminId, id, TODAY.plusDays(3).atTime(10, 0), PaymentMethod.CASH);

        List<String> trail = logs.matching("target=Booking#" + id);
        assertTrue(has(trail, "action=BOOKING_CREATED outcome=SUCCESS actor=user:" + customerId), "created");
        assertTrue(has(trail, "action=PAYMENT outcome=SUCCESS actor=user:" + customerId), "paid");
        assertTrue(has(trail, "action=VEHICLE_PICKED_UP outcome=SUCCESS actor=user:" + adminId), "picked up");
        assertTrue(has(trail, "action=LATE_FEE_CHARGED outcome=SUCCESS"), "late fee");
        assertTrue(has(trail, "action=VEHICLE_RETURNED outcome=SUCCESS"), "returned");
    }

    public void testRejectedAndDeniedAttemptsAreAuditedToo() {
        var bookings = system.bookingService();
        Long id = bookings.createBooking(new CreateBookingRequest(customerId, carId, TODAY, TODAY)).getId();
        tryRun(() -> bookings.createBooking(new CreateBookingRequest(otherId, carId, TODAY, TODAY)));       // overlap
        tryRun(() -> bookings.cancelBooking(id, otherId));                                                 // not owner
        tryRun(() -> system.userService().login(new LoginRequest("riya.sharma@test.com", "wrong-one")));   // bad login
        tryRun(() -> system.vehicleService().changeStatus(customerId, carId, null));                       // not admin

        assertTrue(has(logs.audit(), "action=BOOKING_REJECTED outcome=FAILURE actor=user:" + otherId), "overlap audited");
        assertTrue(has(logs.audit(), "action=BOOKING_CANCELLED outcome=DENIED actor=user:" + otherId), "not owner audited");
        assertTrue(has(logs.audit(), "action=LOGIN outcome=FAILURE actor=user:" + customerId), "bad login audited");
        assertTrue(has(logs.audit(), "action=ACCESS_DENIED outcome=DENIED actor=user:" + customerId), "non-admin audited");
    }

    public void testEveryLineOfOneRequestSharesItsTraceId() {
        ApiResponse<BookingResponse> response = system.bookingController().create(
                () -> new CreateBookingRequest(customerId, carId, TODAY.plusDays(1), TODAY.plusDays(2)));
        String trace = response.traceId();
        assertTrue(trace != null && trace.length() == 8, "response carries a trace id");

        List<String> lines = logs.matching("trace=" + trace);
        assertTrue(has(lines, "--> POST /api/v1/bookings"), "request start logged");
        assertTrue(has(lines, "Overlap check"), "debug decision logged");
        assertTrue(has(lines, "action=BOOKING_CREATED"), "audit carries same trace");
        assertTrue(has(lines, "<-- POST /api/v1/bookings 201"), "request end logged");
    }

    public void testErrorResponseCarriesTraceIdThatMatchesTheWarnLog() {
        ApiResponse<BookingResponse> response = system.bookingController().create(
                () -> new CreateBookingRequest(customerId, carId, TODAY.minusDays(1), TODAY));
        assertEquals(400, response.status());
        assertTrue(has(logs.matching("trace=" + response.traceId()), "| WARN  |"), "WARN line with same trace id");
    }

    public void testTraceIdDoesNotLeakIntoTheNextRequest() {
        String first = system.bookingController().history(customerId).traceId();
        String second = system.bookingController().history(customerId).traceId();
        assertTrue(!first.equals(second), "each request gets a new trace id");
        assertEquals("-", com.jyoti.carrental.logging.TraceContext.current());
    }

    public void testNoPasswordsHashesOrFullEmailsInLogs() {
        system.authController().login(() -> new LoginRequest("riya.sharma@test.com", "secret123"));
        String hash = system.userService().getUser(customerId).getPasswordHash();
        for (String line : logs.lines()) {
            assertTrue(!line.contains("secret123"), "raw password logged: " + line);
            assertTrue(!line.contains(hash), "password hash logged: " + line);
            assertTrue(!line.contains("riya.sharma@test.com"), "full email logged: " + line);
        }
        assertEquals("r***@test.com", LogSanitizer.maskEmail("riya.sharma@test.com"));
        assertEquals("***********1234", LogSanitizer.maskTail("MH1220190001234"));
    }

    public void testExpiryJobRunsUnderItsOwnTraceAndIsAudited() {
        system.bookingService().createBooking(new CreateBookingRequest(customerId, carId, TODAY.plusDays(4), TODAY.plusDays(5)));
        clock.advance(Duration.ofMinutes(16));
        assertEquals(1, system.cleanupJob().runOnce());
        List<String> expiry = logs.matching("action=BOOKING_EXPIRED");
        assertEquals(1, expiry.size());
        assertTrue(expiry.get(0).contains("trace=job-") && expiry.get(0).contains("actor=system"), "job trace + system actor");
    }

    // ---------------------------------------------------------------- helpers

    private Long register(String email, UserRole role) {
        return system.userService().register(new RegisterUserRequest("User", email, "9999999999", "secret123",
                role == UserRole.CUSTOMER ? "DL-123456" : null, role)).getId();
    }

    private static boolean has(List<String> lines, String text) {
        return lines.stream().anyMatch(l -> l.contains(text));
    }

    private static void tryRun(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException expected) {
            // the test checks the audit trail, not the exception
        }
    }
}
