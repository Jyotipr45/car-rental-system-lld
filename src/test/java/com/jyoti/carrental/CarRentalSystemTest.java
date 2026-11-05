package com.jyoti.carrental;

import com.jyoti.carrental.config.CarRentalSystem;
import com.jyoti.carrental.dto.request.AddBikeRequest;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.CreateBookingRequest;
import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.dto.request.LoginRequest;
import com.jyoti.carrental.dto.request.PaymentRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.dto.request.VehicleSearchRequest;
import com.jyoti.carrental.exception.AccessDeniedException;
import com.jyoti.carrental.exception.AuthenticationException;
import com.jyoti.carrental.exception.DuplicateResourceException;
import com.jyoti.carrental.exception.InvalidBookingStateException;
import com.jyoti.carrental.exception.InvalidRequestException;
import com.jyoti.carrental.exception.PaymentFailedException;
import com.jyoti.carrental.exception.VehicleNotAvailableException;
import com.jyoti.carrental.model.Booking;
import com.jyoti.carrental.model.Car;
import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.Vehicle;
import com.jyoti.carrental.model.enums.BookingStatus;
import com.jyoti.carrental.model.enums.CarType;
import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.PaymentPurpose;
import com.jyoti.carrental.model.enums.PaymentStatus;
import com.jyoti.carrental.model.enums.TransmissionType;
import com.jyoti.carrental.model.enums.UserRole;
import com.jyoti.carrental.model.enums.VehicleCategory;
import com.jyoti.carrental.model.enums.VehicleStatus;
import com.jyoti.carrental.service.BookingService;
import com.jyoti.carrental.service.VehicleService;
import com.jyoti.carrental.strategy.pricing.DailyPricingStrategy;
import com.jyoti.carrental.strategy.pricing.WeekendPricingStrategy;
import com.jyoti.carrental.support.MiniTest;
import com.jyoti.carrental.support.MutableClock;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jyoti.carrental.support.MiniTest.assertEquals;
import static com.jyoti.carrental.support.MiniTest.assertThrows;
import static com.jyoti.carrental.support.MiniTest.assertTrue;

/**
 * Behaviour tests for every functional requirement and the key non-functional ones.
 * Each test gets a brand-new system and a fake clock fixed at Monday 2 Nov 2026, 10:00 IST.
 *
 * <p>Run: see README "Run the tests".</p>
 *
 * @author Jyoti Prakash Parida
 */
public class CarRentalSystemTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    /** Monday. Chosen so weekend-pricing tests are predictable. */
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private final MutableClock clock = new MutableClock(TODAY.atTime(10, 0).atZone(IST).toInstant(), IST);
    private final CarRentalSystem system = CarRentalSystem.create(clock, false);
    private final BookingService bookings = system.bookingService();
    private final VehicleService vehicles = system.vehicleService();

    private final Long adminId;
    private final Long customerId;
    private final Long otherCustomerId;
    private final Long branchId;
    private final Long sedanId;  // Rs.2000/day
    private final Long suvId;    // Rs.3000/day

    public CarRentalSystemTest() {
        adminId = register("admin@test.com", UserRole.ADMIN);
        customerId = register("riya@test.com", UserRole.CUSTOMER);
        otherCustomerId = register("arjun@test.com", UserRole.CUSTOMER);
        branchId = system.branchService().createBranch(adminId,
                new CreateBranchRequest("Baner", "Baner Road", "Pune")).getId();
        sedanId = addCar("MH12AA0001", CarType.SEDAN, "2000");
        suvId = addCar("MH12AA0002", CarType.SUV, "3000");
    }

    static {
        // Behaviour tests don't assert on logs: record nothing, print nothing.
        com.jyoti.carrental.logging.LoggingConfig.init(java.util.logging.Level.OFF, java.util.logging.Level.OFF, null);
    }

    public static void main(String[] args) throws Exception {
        MiniTest.run(CarRentalSystemTest.class);
    }

    // ================================================================ users & access

    public void testRegisterHashesPasswordAndLoginWorks() {
        var user = system.userService().getUser(customerId);
        assertTrue(!user.getPasswordHash().contains("secret123"), "password must be hashed");
        assertEquals(customerId, system.userService().login(new LoginRequest("riya@test.com", "secret123")).getId());
    }

    public void testLoginWithWrongPasswordFails() {
        assertThrows(AuthenticationException.class,
                () -> system.userService().login(new LoginRequest("riya@test.com", "wrong!!")));
    }

    public void testDuplicateEmailRejected() {
        assertThrows(DuplicateResourceException.class, () -> register("RIYA@test.com", UserRole.CUSTOMER));
    }

    public void testCustomerNeedsDrivingLicence() {
        assertThrows(InvalidRequestException.class, () -> new RegisterUserRequest("X", "x@test.com", "1",
                "secret123", null, UserRole.CUSTOMER));
    }

    public void testCustomerCannotAddVehicle() {
        assertThrows(AccessDeniedException.class, () -> vehicles.addCar(customerId, carRequest("MH12ZZ9999",
                CarType.SUV, "1000")));
    }

    public void testDuplicateRegistrationNumberRejected() {
        assertThrows(DuplicateResourceException.class, () -> addCar("mh12aa0001", CarType.SUV, "1000"));
    }

    // ================================================================ search

    public void testSearchReturnsOnlyFreeRentableVehiclesSortedByPrice() {
        vehicles.addBike(adminId, new AddBikeRequest("MH12BB0001", "Honda", "Activa", FuelType.PETROL,
                new BigDecimal("500"), branchId, 110));
        book(customerId, suvId, TODAY.plusDays(1), TODAY.plusDays(3));

        List<Vehicle> found = vehicles.searchAvailable(VehicleSearchRequest.of(branchId, TODAY.plusDays(2), TODAY.plusDays(2)));
        assertEquals(2, found.size());                      // bike + sedan, SUV is booked
        assertEquals(VehicleCategory.BIKE, found.get(0).getCategory()); // cheapest first
    }

    public void testSearchFiltersByCarTypeAndHidesMaintenance() {
        var suvs = vehicles.searchAvailable(new VehicleSearchRequest(branchId, TODAY, TODAY.plusDays(1),
                VehicleCategory.CAR, CarType.SUV, null));
        assertEquals(1, suvs.size());
        vehicles.changeStatus(adminId, suvId, VehicleStatus.UNDER_MAINTENANCE);
        assertEquals(0, vehicles.searchAvailable(new VehicleSearchRequest(branchId, TODAY, TODAY.plusDays(1),
                VehicleCategory.CAR, CarType.SUV, null)).size());
    }

    // ================================================================ booking rules

    public void testBookingPriceIsDailyRateTimesInclusiveDays() {
        Booking b = book(customerId, sedanId, TODAY.plusDays(1), TODAY.plusDays(3)); // 3 days
        assertEquals(new BigDecimal("6000.00"), b.getTotalAmount());
        assertEquals(BookingStatus.PENDING_PAYMENT, b.getStatus());
    }

    public void testOverlappingBookingRejectedButAdjacentAllowed() {
        book(customerId, sedanId, TODAY.plusDays(1), TODAY.plusDays(3));
        assertThrows(VehicleNotAvailableException.class,
                () -> book(otherCustomerId, sedanId, TODAY.plusDays(3), TODAY.plusDays(5))); // shares day 3
        Booking adjacent = book(otherCustomerId, sedanId, TODAY.plusDays(4), TODAY.plusDays(5));
        assertEquals(BookingStatus.PENDING_PAYMENT, adjacent.getStatus());
    }

    public void testCancelledBookingFreesTheDates() {
        Booking b = book(customerId, sedanId, TODAY.plusDays(1), TODAY.plusDays(2));
        bookings.cancelBooking(b.getId(), customerId);
        book(otherCustomerId, sedanId, TODAY.plusDays(1), TODAY.plusDays(2)); // no exception
    }

    public void testInvalidDatesRejected() {
        assertThrows(InvalidRequestException.class, () -> book(customerId, sedanId, TODAY.minusDays(1), TODAY));
        assertThrows(InvalidRequestException.class, () -> book(customerId, sedanId, TODAY.plusDays(3), TODAY.plusDays(1)));
        assertThrows(InvalidRequestException.class, () -> book(customerId, sedanId, TODAY, TODAY.plusDays(30))); // 31 days
    }

    public void testAdminCannotCreateBookingForThemself() {
        assertThrows(AccessDeniedException.class, () -> book(adminId, sedanId, TODAY, TODAY));
    }

    public void testVehicleUnderMaintenanceCannotBeBooked() {
        vehicles.changeStatus(adminId, sedanId, VehicleStatus.UNDER_MAINTENANCE);
        assertThrows(VehicleNotAvailableException.class, () -> book(customerId, sedanId, TODAY, TODAY));
    }

    // ================================================================ payment

    public void testSuccessfulPaymentConfirmsBooking() {
        Booking b = book(customerId, sedanId, TODAY, TODAY.plusDays(1));
        Payment p = bookings.payForBooking(new PaymentRequest(b.getId(), customerId, PaymentMethod.UPI));
        assertEquals(PaymentStatus.SUCCESS, p.getStatus());
        assertTrue(p.getTransactionRef().startsWith("UPI-"), "UPI processor must be chosen by the factory");
        assertEquals(BookingStatus.CONFIRMED, bookings.getBooking(b.getId()).getStatus());
    }

    public void testDeclinedPaymentKeepsBookingPendingAndIsAudited() {
        Long luxuryId = addCar("MH12LX0001", CarType.LUXURY, "60000");
        Booking b = book(customerId, luxuryId, TODAY, TODAY.plusDays(1)); // Rs.1,20,000 > UPI limit
        assertThrows(PaymentFailedException.class,
                () -> bookings.payForBooking(new PaymentRequest(b.getId(), customerId, PaymentMethod.UPI)));
        assertEquals(BookingStatus.PENDING_PAYMENT, bookings.getBooking(b.getId()).getStatus());
        assertEquals(PaymentStatus.FAILED, bookings.getPayments(b.getId()).get(0).getStatus());
        // retry with card succeeds
        bookings.payForBooking(new PaymentRequest(b.getId(), customerId, PaymentMethod.CARD));
        assertEquals(BookingStatus.CONFIRMED, bookings.getBooking(b.getId()).getStatus());
    }

    public void testCannotPaySomeoneElsesBookingOrPayTwice() {
        Booking b = book(customerId, sedanId, TODAY, TODAY);
        assertThrows(AccessDeniedException.class,
                () -> bookings.payForBooking(new PaymentRequest(b.getId(), otherCustomerId, PaymentMethod.CARD)));
        bookings.payForBooking(new PaymentRequest(b.getId(), customerId, PaymentMethod.CARD));
        assertThrows(InvalidBookingStateException.class,
                () -> bookings.payForBooking(new PaymentRequest(b.getId(), customerId, PaymentMethod.CARD)));
    }

    // ================================================================ cancel

    public void testCancelConfirmedBookingRefundsPayment() {
        Booking b = confirmedBooking(sedanId, TODAY.plusDays(1), TODAY.plusDays(2));
        bookings.cancelBooking(b.getId(), customerId);
        assertEquals(BookingStatus.CANCELLED, bookings.getBooking(b.getId()).getStatus());
        assertEquals(PaymentStatus.REFUNDED, bookings.getPayments(b.getId()).get(0).getStatus());
    }

    public void testOtherCustomerCannotCancel() {
        Booking b = book(customerId, sedanId, TODAY, TODAY);
        assertThrows(AccessDeniedException.class, () -> bookings.cancelBooking(b.getId(), otherCustomerId));
    }

    public void testCannotCancelAfterPickup() {
        Booking b = confirmedBooking(sedanId, TODAY, TODAY.plusDays(1));
        bookings.markPickup(adminId, b.getId());
        assertThrows(InvalidBookingStateException.class, () -> bookings.cancelBooking(b.getId(), customerId));
    }

    // ================================================================ pickup & return

    public void testPickupOnlyFromStartDate() {
        Booking b = confirmedBooking(sedanId, TODAY.plusDays(2), TODAY.plusDays(3));
        assertThrows(InvalidRequestException.class, () -> bookings.markPickup(adminId, b.getId()));
        clock.advance(Duration.ofDays(2));
        assertEquals(BookingStatus.ONGOING, bookings.markPickup(adminId, b.getId()).getStatus());
    }

    public void testUnpaidBookingCannotBePickedUp() {
        Booking b = book(customerId, sedanId, TODAY, TODAY);
        assertThrows(InvalidBookingStateException.class, () -> bookings.markPickup(adminId, b.getId()));
    }

    public void testOnTimeReturnHasNoLateFee() {
        Booking b = confirmedBooking(sedanId, TODAY, TODAY.plusDays(1));
        bookings.markPickup(adminId, b.getId());
        Booking done = bookings.markReturn(adminId, b.getId(), TODAY.plusDays(1).atTime(22, 0), null);
        assertEquals(BookingStatus.COMPLETED, done.getStatus());
        assertEquals(new BigDecimal("0.00"), done.getLateFee());
        assertEquals(new BigDecimal("4000.00"), done.getTotalAmount());
    }

    public void testLateReturnChargesOneAndHalfTimesDailyRatePerLateDay() {
        Booking b = confirmedBooking(sedanId, TODAY, TODAY.plusDays(1));      // Rs.4000
        bookings.markPickup(adminId, b.getId());
        LocalDateTime twoDaysLate = TODAY.plusDays(3).atTime(9, 0);
        Booking done = bookings.markReturn(adminId, b.getId(), twoDaysLate, PaymentMethod.CASH);
        assertEquals(new BigDecimal("6000.00"), done.getLateFee());            // 2 x 2000 x 1.5
        assertEquals(new BigDecimal("10000.00"), done.getTotalAmount());
        Payment fee = bookings.getPayments(b.getId()).get(1);
        assertEquals(PaymentPurpose.LATE_FEE, fee.getPurpose());
        assertEquals(PaymentStatus.SUCCESS, fee.getStatus());
    }

    public void testCompletedBookingIsFinal() {
        Booking b = confirmedBooking(sedanId, TODAY, TODAY);
        bookings.markPickup(adminId, b.getId());
        bookings.markReturn(adminId, b.getId(), TODAY.atTime(20, 0), null);
        assertThrows(InvalidBookingStateException.class,
                () -> bookings.markReturn(adminId, b.getId(), TODAY.atTime(21, 0), null));
        assertTrue(!BookingStatus.COMPLETED.canTransitionTo(BookingStatus.ONGOING), "COMPLETED must be final");
    }

    // ================================================================ expiry job

    public void testUnpaidBookingExpiresAfterPaymentWindow() {
        Booking unpaid = book(customerId, sedanId, TODAY.plusDays(1), TODAY.plusDays(2));
        Booking paid = confirmedBooking(suvId, TODAY.plusDays(1), TODAY.plusDays(2));

        clock.advance(Duration.ofMinutes(10));
        assertEquals(0, system.cleanupJob().runOnce());                       // still inside 15-min window
        clock.advance(Duration.ofMinutes(6));
        assertEquals(1, system.cleanupJob().runOnce());

        assertEquals(BookingStatus.CANCELLED, bookings.getBooking(unpaid.getId()).getStatus());
        assertEquals(BookingStatus.CONFIRMED, bookings.getBooking(paid.getId()).getStatus());
        book(otherCustomerId, sedanId, TODAY.plusDays(1), TODAY.plusDays(2)); // car is free again
    }

    // ================================================================ concurrency (NFR)

    public void testConcurrentBookingsForSameVehicleOnlyOneWins() throws Exception {
        int threads = 32;
        List<Long> customers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            customers.add(register("racer" + i + "@test.com", UserRole.CUSTOMER));
        }
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());
        for (Long c : customers) {
            pool.submit(() -> {
                try {
                    go.await();
                    book(c, sedanId, TODAY.plusDays(5), TODAY.plusDays(6));
                    success.incrementAndGet();
                } catch (VehicleNotAvailableException e) {
                    conflict.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            });
        }
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "threads did not finish");
        assertEquals(List.of(), unexpected);
        assertEquals(1, success.get());
        assertEquals(threads - 1, conflict.get());
    }

    // ================================================================ strategies

    public void testWeekendPricingStrategyChargesSurchargeOnSatSun() {
        Vehicle car = vehicles.getVehicle(sedanId);                  // Rs.2000/day
        LocalDate friday = LocalDate.of(2026, 11, 6);
        LocalDate sunday = LocalDate.of(2026, 11, 8);
        assertEquals(new BigDecimal("6000.00"), new DailyPricingStrategy().calculatePrice(car, friday, sunday));
        // Fri 2000 + Sat 2500 + Sun 2500
        assertEquals(new BigDecimal("7000.00"),
                new WeekendPricingStrategy(new BigDecimal("1.25")).calculatePrice(car, friday, sunday));
    }

    public void testBookingHistoryBelongsToUser() {
        book(customerId, sedanId, TODAY, TODAY);
        book(otherCustomerId, suvId, TODAY, TODAY);
        assertEquals(1, bookings.getUserBookings(customerId).size());
    }

    // ================================================================ helpers

    private Long register(String email, UserRole role) {
        return system.userService().register(new RegisterUserRequest("User " + email, email, "9999999999",
                "secret123", role == UserRole.CUSTOMER ? "DL-" + email : null, role)).getId();
    }

    private AddCarRequest carRequest(String reg, CarType type, String price) {
        return new AddCarRequest(reg, "Brand", "Model", FuelType.PETROL, new BigDecimal(price), branchId,
                type, TransmissionType.AUTOMATIC, 5);
    }

    private Long addCar(String reg, CarType type, String price) {
        Vehicle v = vehicles.addCar(adminId, carRequest(reg, type, price));
        assertTrue(v instanceof Car, "addCar must create a Car");
        return v.getId();
    }

    private Booking book(Long userId, Long vehicleId, LocalDate start, LocalDate end) {
        return bookings.createBooking(new CreateBookingRequest(userId, vehicleId, start, end));
    }

    private Booking confirmedBooking(Long vehicleId, LocalDate start, LocalDate end) {
        Booking b = book(customerId, vehicleId, start, end);
        bookings.payForBooking(new PaymentRequest(b.getId(), customerId, PaymentMethod.CARD));
        return bookings.getBooking(b.getId());
    }
}
