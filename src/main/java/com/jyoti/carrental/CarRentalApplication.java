package com.jyoti.carrental;

import com.jyoti.carrental.config.CarRentalSystem;
import com.jyoti.carrental.controller.AuthController;
import com.jyoti.carrental.controller.BookingController;
import com.jyoti.carrental.controller.BranchController;
import com.jyoti.carrental.controller.VehicleController;
import com.jyoti.carrental.dto.request.AddBikeRequest;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.CreateBookingRequest;
import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.dto.request.LoginRequest;
import com.jyoti.carrental.dto.request.PaymentRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.dto.request.VehicleSearchRequest;
import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.dto.response.BookingResponse;
import com.jyoti.carrental.dto.response.UserResponse;
import com.jyoti.carrental.logging.LoggingConfig;
import com.jyoti.carrental.model.enums.CarType;
import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.TransmissionType;
import com.jyoti.carrental.model.enums.UserRole;
import com.jyoti.carrental.model.enums.VehicleCategory;
import com.jyoti.carrental.model.enums.VehicleStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Entry point. Walks through every functional requirement as a console story, calling
 * the controllers exactly as an HTTP client would call REST endpoints.
 *
 * <p>Run: {@code java -jar target/car-rental-system-1.0.0.jar}</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class CarRentalApplication {

    // Logging is configured before anything else runs, so even startup is logged.
    static {
        LoggingConfig.initFromEnvironment();
    }

    private static final CarRentalSystem SYSTEM = CarRentalSystem.getInstance(); // the singleton
    private static final AuthController AUTH = SYSTEM.authController();
    private static final BranchController BRANCHES = SYSTEM.branchController();
    private static final VehicleController VEHICLES = SYSTEM.vehicleController();
    private static final BookingController BOOKINGS = SYSTEM.bookingController();

    private CarRentalApplication() {
    }

    public static void main(String[] args) throws Exception {
        LocalDate today = LocalDate.now();
        banner("CAR RENTAL SYSTEM: LLD demo by Jyoti Prakash Parida");

        // ------------------------------------------------------------------ 1. setup
        section("1. Admin sets up a branch and its fleet");
        Long adminId = AUTH.register(() -> new RegisterUserRequest("Admin Pune", "admin@rental.com",
                "9000000001", "admin@123", null, UserRole.ADMIN)).data().id();
        Long branchId = show(BRANCHES.create(adminId, () -> new CreateBranchRequest(
                "Hinjewadi Hub", "Phase 1, Hinjewadi", "Pune"))).data().id();

        Long swiftId = show(VEHICLES.addCar(adminId, () -> new AddCarRequest("MH12AB1001", "Maruti", "Swift",
                FuelType.PETROL, new BigDecimal("1800"), branchId, CarType.HATCHBACK, TransmissionType.MANUAL, 5)))
                .data().id();
        Long cretaId = show(VEHICLES.addCar(adminId, () -> new AddCarRequest("MH12AB1002", "Hyundai", "Creta",
                FuelType.DIESEL, new BigDecimal("3200"), branchId, CarType.SUV, TransmissionType.AUTOMATIC, 5)))
                .data().id();
        Long nexonId = show(VEHICLES.addCar(adminId, () -> new AddCarRequest("MH12AB1003", "Tata", "Nexon EV",
                FuelType.ELECTRIC, new BigDecimal("2800"), branchId, CarType.SUV, TransmissionType.AUTOMATIC, 5)))
                .data().id();
        show(VEHICLES.addBike(adminId, () -> new AddBikeRequest("MH12XY9001", "Royal Enfield", "Classic 350",
                FuelType.PETROL, new BigDecimal("900"), branchId, 350)));
        Long bmwId = show(VEHICLES.addCar(adminId, () -> new AddCarRequest("MH12LX0007", "BMW", "7 Series",
                FuelType.PETROL, new BigDecimal("40000"), branchId, CarType.LUXURY, TransmissionType.AUTOMATIC, 5)))
                .data().id();

        // ------------------------------------------------------------------ 2. validation & auth
        section("2. Validation and access control");
        show(AUTH.register(() -> new RegisterUserRequest("No Licence", "nolicence@mail.com",
                "9000000009", "secret12", null, UserRole.CUSTOMER)));                  // 400
        Long riyaId = show(AUTH.register(() -> new RegisterUserRequest("Riya Sharma", "riya@mail.com",
                "9000000002", "riya@123", "MH1220190001234", UserRole.CUSTOMER))).data().id();
        Long arjunId = AUTH.register(() -> new RegisterUserRequest("Arjun Mehta", "arjun@mail.com",
                "9000000003", "arjun@123", "MH1220180005678", UserRole.CUSTOMER)).data().id();
        show(AUTH.login(() -> new LoginRequest("riya@mail.com", "wrong-pass")));      // 401
        show(AUTH.login(() -> new LoginRequest("riya@mail.com", "riya@123")));        // 200
        show(VEHICLES.addCar(riyaId, () -> new AddCarRequest("MH12ZZ0000", "Kia", "Seltos", FuelType.PETROL,
                new BigDecimal("3000"), branchId, CarType.SUV, TransmissionType.MANUAL, 5))); // 403

        // ------------------------------------------------------------------ 3. search
        section("3. Riya searches SUVs for the next 3 days");
        LocalDate start = today;
        LocalDate end = today.plusDays(2);
        show(VEHICLES.search(() -> new VehicleSearchRequest(branchId, start, end,
                VehicleCategory.CAR, CarType.SUV, null)));

        // ------------------------------------------------------------------ 4. happy path
        section("4. Happy path: book -> pay (UPI) -> pickup -> late return");
        Long bookingId = show(BOOKINGS.create(() -> new CreateBookingRequest(riyaId, cretaId, start, end))).data().id();
        show(BOOKINGS.pay(() -> new PaymentRequest(bookingId, riyaId, PaymentMethod.UPI)));
        show(BOOKINGS.pickup(adminId, bookingId));
        LocalDateTime lateReturn = end.plusDays(2).atTime(11, 0); // 2 days late
        show(BOOKINGS.returnVehicle(adminId, bookingId, lateReturn, PaymentMethod.CARD));
        show(BOOKINGS.payments(bookingId));

        // ------------------------------------------------------------------ 5. double booking
        section("5. Double booking is rejected");
        Long nexonBooking = show(BOOKINGS.create(() -> new CreateBookingRequest(arjunId, nexonId,
                today.plusDays(5), today.plusDays(7)))).data().id();
        show(BOOKINGS.create(() -> new CreateBookingRequest(riyaId, nexonId,
                today.plusDays(6), today.plusDays(9))));                                 // 409 overlap
        show(BOOKINGS.create(() -> new CreateBookingRequest(riyaId, nexonId,
                today.plusDays(8), today.plusDays(9))));                                 // 201 no overlap

        // ------------------------------------------------------------------ 6. concurrency
        section("6. Concurrency: 20 customers click 'Book' on the same Swift at the same instant");
        raceForSameCar(adminId, swiftId, today.plusDays(10), today.plusDays(12));

        // ------------------------------------------------------------------ 7. cancel + refund
        section("7. Arjun pays, then cancels -> refund");
        show(BOOKINGS.pay(() -> new PaymentRequest(nexonBooking, arjunId, PaymentMethod.CARD)));
        show(BOOKINGS.cancel(nexonBooking, riyaId));                                   // 403 not owner
        show(BOOKINGS.cancel(nexonBooking, arjunId));                                  // 200
        show(BOOKINGS.payments(nexonBooking));

        // ------------------------------------------------------------------ 8. payment failure
        section("8. UPI limit: Rs.1,20,000 booking is declined, booking stays unpaid, card works");
        Long bmwBooking = show(BOOKINGS.create(() -> new CreateBookingRequest(arjunId, bmwId,
                today.plusDays(20), today.plusDays(22)))).data().id();
        show(BOOKINGS.pay(() -> new PaymentRequest(bmwBooking, arjunId, PaymentMethod.UPI)));   // 402
        show(BOOKINGS.get(bmwBooking));
        show(BOOKINGS.pay(() -> new PaymentRequest(bmwBooking, arjunId, PaymentMethod.CARD)));  // 201

        // ------------------------------------------------------------------ 9. invalid transitions
        section("9. State machine guards");
        show(BOOKINGS.pickup(adminId, nexonBooking));                                  // cancelled → ongoing: 409
        show(BOOKINGS.cancel(bookingId, riyaId));                                      // completed → cancelled: 409
        show(BOOKINGS.create(() -> new CreateBookingRequest(riyaId, swiftId,
                today.minusDays(1), today.plusDays(1))));                                // past date: 400

        // ------------------------------------------------------------------ 10. maintenance
        section("10. Vehicle under maintenance disappears from search");
        show(VEHICLES.changeStatus(adminId, cretaId, VehicleStatus.UNDER_MAINTENANCE));
        show(VEHICLES.search(() -> VehicleSearchRequest.of(branchId, today.plusDays(25), today.plusDays(26))));

        // ------------------------------------------------------------------ 11. expiry job
        section("11. Unpaid bookings expire (payment window = 0 for the demo)");
        show(BOOKINGS.create(() -> new CreateBookingRequest(riyaId, swiftId, today.plusDays(15), today.plusDays(16))));
        int expired = SYSTEM.bookingService().expireUnpaidBookings(Duration.ZERO);
        System.out.println("  Expired bookings: " + expired);

        // ------------------------------------------------------------------ 12. history
        section("12. Riya's booking history");
        show(BOOKINGS.history(riyaId));

        banner("Demo finished");
        System.out.println("  Full application log : logs/car-rental-0.log");
        System.out.println("  Audit trail          : logs/audit-0.log");
        System.out.println("  Tip: run with 'run.bat debug' to stream DEBUG/TRACE logs to the console.");
    }

    /** Fires many booking requests at once and proves only one wins. */
    private static void raceForSameCar(Long adminId, Long vehicleId, LocalDate start, LocalDate end)
            throws InterruptedException {
        int threads = 20;
        List<Long> customers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int n = i;
            UserResponse u = AUTH.register(() -> new RegisterUserRequest("Racer " + n, "racer" + n + "@mail.com",
                    "98000000" + String.format("%02d", n), "racer@123", "DL" + n, UserRole.CUSTOMER)).data();
            customers.add(u.id());
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<ApiResponse<BookingResponse>> results = java.util.Collections.synchronizedList(new ArrayList<>());
        for (Long customerId : customers) {
            pool.submit(() -> {
                startGun.await(); // all threads wait here, then go together
                results.add(BOOKINGS.create(() -> new CreateBookingRequest(customerId, vehicleId, start, end)));
                return null;
            });
        }
        startGun.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        long won = results.stream().filter(ApiResponse::success).count();
        long rejected = results.stream().filter(r -> r.status() == 409).count();
        System.out.println("  Requests: " + results.size() + " | succeeded: " + won + " | rejected with 409: " + rejected);
        System.out.println(won == 1 ? "  [OK] Exactly one booking won. No double booking."
                                    : "  [FAIL] Double booking happened!");
    }

    // ---------------------------------------------------------------- console helpers

    private static <T> ApiResponse<T> show(ApiResponse<T> response) {
        System.out.println("  " + response);
        return response;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("-- " + title + " " + "-".repeat(Math.max(0, 78 - title.length())));
    }

    private static void banner(String text) {
        System.out.println();
        System.out.println("=".repeat(82));
        System.out.println("  " + text);
        System.out.println("=".repeat(82));
    }
}
