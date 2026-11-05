package com.jyoti.carrental.config;

import com.jyoti.carrental.controller.AuthController;
import com.jyoti.carrental.controller.BookingController;
import com.jyoti.carrental.controller.BranchController;
import com.jyoti.carrental.controller.GlobalExceptionHandler;
import com.jyoti.carrental.controller.VehicleController;
import com.jyoti.carrental.event.ConsoleNotificationListener;
import com.jyoti.carrental.payment.CardPaymentProcessor;
import com.jyoti.carrental.payment.CashPaymentProcessor;
import com.jyoti.carrental.payment.PaymentProcessorFactory;
import com.jyoti.carrental.payment.UpiPaymentProcessor;
import com.jyoti.carrental.repository.BookingRepository;
import com.jyoti.carrental.repository.BranchRepository;
import com.jyoti.carrental.repository.PaymentRepository;
import com.jyoti.carrental.repository.UserRepository;
import com.jyoti.carrental.repository.VehicleRepository;
import com.jyoti.carrental.scheduler.PendingBookingCleanupJob;
import com.jyoti.carrental.service.BookingService;
import com.jyoti.carrental.service.BranchService;
import com.jyoti.carrental.service.PaymentService;
import com.jyoti.carrental.service.UserService;
import com.jyoti.carrental.service.VehicleService;
import com.jyoti.carrental.service.impl.BookingServiceImpl;
import com.jyoti.carrental.service.impl.BranchServiceImpl;
import com.jyoti.carrental.service.impl.PaymentServiceImpl;
import com.jyoti.carrental.service.impl.UserServiceImpl;
import com.jyoti.carrental.service.impl.VehicleServiceImpl;
import com.jyoti.carrental.strategy.latefee.LateFeePolicy;
import com.jyoti.carrental.strategy.latefee.StandardLateFeePolicy;
import com.jyoti.carrental.strategy.pricing.DailyPricingStrategy;
import com.jyoti.carrental.strategy.pricing.PricingStrategy;
import com.jyoti.carrental.util.VehicleLockManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * The application's composition root: creates every object once and wires them together.
 *
 * <p><b>Singleton pattern (hand-written).</b> {@link #getInstance()} uses double-checked
 * locking with a {@code volatile} field: the first caller builds the system, everyone
 * else gets the same instance, and {@code volatile} stops other threads from seeing a
 * half-built object.</p>
 *
 * <p><b>Why only one hand-written singleton?</b> Repositories, services, strategies and
 * the factory are created exactly once <i>here</i> and shared, which is what makes them
 * singletons in practice. That is precisely what the Spring container does for every
 * {@code @Service}/{@code @Repository} bean (default scope = singleton). Making each class
 * enforce its own singleton would hide dependencies and make testing hard.</p>
 *
 * <p>Tests use {@link #create(Clock, boolean)} to get a fresh, isolated system with a fake clock.</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class CarRentalSystem {

    /** How long a customer has to pay before an unpaid booking is released. */
    public static final Duration PAYMENT_WINDOW = Duration.ofMinutes(15);

    private static volatile CarRentalSystem instance;

    // ---- services (interfaces, so callers never see the Impl classes)
    private final UserService userService;
    private final BranchService branchService;
    private final VehicleService vehicleService;
    private final PaymentService paymentService;
    private final BookingService bookingService;

    // ---- controllers (the "API" surface)
    private final AuthController authController;
    private final BranchController branchController;
    private final VehicleController vehicleController;
    private final BookingController bookingController;

    private final PendingBookingCleanupJob cleanupJob;

    private CarRentalSystem(Clock clock, boolean consoleNotifications) {
        // 1. Repositories (data layer)
        UserRepository userRepository = new UserRepository(clock);
        BranchRepository branchRepository = new BranchRepository(clock);
        VehicleRepository vehicleRepository = new VehicleRepository(clock);
        BookingRepository bookingRepository = new BookingRepository(clock);
        PaymentRepository paymentRepository = new PaymentRepository(clock);

        // 2. Strategies: change pricing for the whole system by swapping ONE line here,
        //    e.g. new WeekendPricingStrategy(new BigDecimal("1.25"))
        PricingStrategy pricingStrategy = new DailyPricingStrategy();
        LateFeePolicy lateFeePolicy = new StandardLateFeePolicy(new BigDecimal("1.5"));

        // 3. Payment processors behind a factory. Add NetBanking = add one entry.
        PaymentProcessorFactory processorFactory = new PaymentProcessorFactory(List.of(
                new CardPaymentProcessor(), new UpiPaymentProcessor(), new CashPaymentProcessor()));

        // 4. Services (business layer), dependencies passed through constructors
        this.userService = new UserServiceImpl(userRepository);
        this.branchService = new BranchServiceImpl(branchRepository, userService);
        this.vehicleService = new VehicleServiceImpl(vehicleRepository, bookingRepository,
                branchService, userService, clock);
        this.paymentService = new PaymentServiceImpl(paymentRepository, processorFactory, clock);
        this.bookingService = new BookingServiceImpl(bookingRepository, userService, vehicleService,
                paymentService, pricingStrategy, lateFeePolicy, new VehicleLockManager(), clock);

        // 5. Observers
        bookingService.addListener(new ConsoleNotificationListener(consoleNotifications));

        // 6. Controllers (presentation layer)
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        this.authController = new AuthController(userService, handler);
        this.branchController = new BranchController(branchService, handler);
        this.vehicleController = new VehicleController(vehicleService, handler);
        this.bookingController = new BookingController(bookingService, handler);

        // 7. Background jobs
        this.cleanupJob = new PendingBookingCleanupJob(bookingService, PAYMENT_WINDOW);
    }

    /** The one shared instance for the running application (real clock, console notifications on). */
    public static CarRentalSystem getInstance() {
        if (instance == null) {                       // 1st check, no locking (fast path)
            synchronized (CarRentalSystem.class) {
                if (instance == null) {               // 2nd check, inside the lock
                    instance = new CarRentalSystem(Clock.systemDefaultZone(), true);
                }
            }
        }
        return instance;
    }

    /** A fresh, independent system. Use in tests so each test starts clean. */
    public static CarRentalSystem create(Clock clock, boolean consoleNotifications) {
        return new CarRentalSystem(clock, consoleNotifications);
    }

    public UserService userService() { return userService; }
    public BranchService branchService() { return branchService; }
    public VehicleService vehicleService() { return vehicleService; }
    public PaymentService paymentService() { return paymentService; }
    public BookingService bookingService() { return bookingService; }

    public AuthController authController() { return authController; }
    public BranchController branchController() { return branchController; }
    public VehicleController vehicleController() { return vehicleController; }
    public BookingController bookingController() { return bookingController; }

    public PendingBookingCleanupJob cleanupJob() { return cleanupJob; }
}
