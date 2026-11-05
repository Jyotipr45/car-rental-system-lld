package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.dto.request.CreateBookingRequest;
import com.jyoti.carrental.dto.request.PaymentRequest;
import com.jyoti.carrental.event.BookingEvent;
import com.jyoti.carrental.event.BookingEventListener;
import com.jyoti.carrental.event.BookingEventType;
import com.jyoti.carrental.exception.AccessDeniedException;
import com.jyoti.carrental.exception.InvalidBookingStateException;
import com.jyoti.carrental.exception.InvalidRequestException;
import com.jyoti.carrental.exception.PaymentFailedException;
import com.jyoti.carrental.exception.ResourceNotFoundException;
import com.jyoti.carrental.exception.VehicleNotAvailableException;
import com.jyoti.carrental.logging.AuditAction;
import com.jyoti.carrental.logging.AuditLogger;
import com.jyoti.carrental.model.Booking;
import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.User;
import com.jyoti.carrental.model.Vehicle;
import com.jyoti.carrental.model.enums.BookingStatus;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.PaymentPurpose;
import com.jyoti.carrental.model.enums.PaymentStatus;
import com.jyoti.carrental.model.enums.UserRole;
import com.jyoti.carrental.repository.BookingRepository;
import com.jyoti.carrental.service.BookingService;
import com.jyoti.carrental.service.PaymentService;
import com.jyoti.carrental.service.UserService;
import com.jyoti.carrental.service.VehicleService;
import com.jyoti.carrental.strategy.latefee.LateFeePolicy;
import com.jyoti.carrental.strategy.pricing.PricingStrategy;
import com.jyoti.carrental.util.VehicleLockManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default {@link BookingService}: orchestrates users, vehicles, pricing, payments and events.
 *
 * <p><b>Concurrency rule:</b> every change to a booking runs inside
 * {@link VehicleLockManager#withLock} for that booking's vehicle. So "check availability
 * then save", "pay", "cancel" and "expire" can never interleave for the same vehicle.</p>
 *
 * <p><b>Strategies are injected</b> ({@link PricingStrategy}, {@link LateFeePolicy}); this
 * class never knows which concrete rule is active.</p>
 *
 * <p><b>Logging:</b> INFO for each completed business step, DEBUG for the decisions behind
 * it (overlap check, price, late fee), and one audit record per action, including
 * rejected and denied attempts.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class BookingServiceImpl implements BookingService {

    private static final Logger LOG = Logger.getLogger(BookingServiceImpl.class.getName());

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final PaymentService paymentService;
    private final PricingStrategy pricingStrategy;
    private final LateFeePolicy lateFeePolicy;
    private final VehicleLockManager lockManager;
    private final AuditLogger audit;
    private final Clock clock;
    /** Thread-safe list because listeners may be added while events are published. */
    private final List<BookingEventListener> listeners = new CopyOnWriteArrayList<>();

    public BookingServiceImpl(BookingRepository bookingRepository, UserService userService,
                              VehicleService vehicleService, PaymentService paymentService,
                              PricingStrategy pricingStrategy, LateFeePolicy lateFeePolicy,
                              VehicleLockManager lockManager, AuditLogger audit, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.paymentService = paymentService;
        this.pricingStrategy = pricingStrategy;
        this.lateFeePolicy = lateFeePolicy;
        this.lockManager = lockManager;
        this.audit = audit;
        this.clock = clock;
    }

    // ================================================================ create

    @Override
    public Booking createBooking(CreateBookingRequest request) {
        LOG.fine(() -> "createBooking user=" + request.userId() + " vehicle=" + request.vehicleId()
                + " dates=" + request.startDate() + ".." + request.endDate());
        User user = userService.getUser(request.userId());
        if (user.getRole() != UserRole.CUSTOMER) {
            audit.denied(AuditAction.BOOKING_CREATED, user.getId(), "Vehicle#" + request.vehicleId(), "role=" + user.getRole());
            throw new AccessDeniedException("Only customers can create bookings");
        }
        Vehicle vehicle = vehicleService.getVehicle(request.vehicleId());
        BookingRules.validateDates(request.startDate(), request.endDate(), today());

        // Critical section: availability check + save must be atomic per vehicle.
        Booking saved = lockManager.withLock(vehicle.getId(), () -> {
            if (!vehicle.isRentable()) {
                reject(user, vehicle, request, "vehicle is " + vehicle.getStatus());
                throw new VehicleNotAvailableException("Vehicle " + vehicle.getId() + " is " + vehicle.getStatus());
            }
            // Always re-check here: the search result the customer saw may already be stale.
            boolean overlap = bookingRepository.existsActiveOverlap(vehicle.getId(), request.startDate(), request.endDate());
            LOG.fine(() -> "Overlap check Vehicle#" + vehicle.getId() + " " + request.startDate() + ".."
                    + request.endDate() + " -> " + (overlap ? "CONFLICT" : "free"));
            if (overlap) {
                reject(user, vehicle, request, "overlaps an active booking");
                throw new VehicleNotAvailableException("Vehicle " + vehicle.getId()
                        + " is already booked between " + request.startDate() + " and " + request.endDate());
            }
            BigDecimal price = pricingStrategy.calculatePrice(vehicle, request.startDate(), request.endDate());
            LOG.fine(() -> "Price via " + pricingStrategy.name() + ": Rs." + price);
            Booking booking = new Booking(user.getId(), vehicle.getId(), vehicle.getBranchId(),
                    request.startDate(), request.endDate(), price);
            return bookingRepository.save(booking);
        });

        LOG.info(() -> "Booking#" + saved.getId() + " created: User#" + user.getId() + " Vehicle#" + vehicle.getId()
                + " " + saved.getStartDate() + ".." + saved.getEndDate() + " Rs." + saved.getTotalAmount()
                + " [" + saved.getStatus() + "]");
        audit.success(AuditAction.BOOKING_CREATED, user.getId(), "Booking#" + saved.getId(),
                "vehicle=" + vehicle.getId() + " " + saved.getStartDate() + ".." + saved.getEndDate()
                        + " amount=" + saved.getTotalAmount());
        publish(BookingEventType.CREATED, saved);
        return saved;
    }

    private void reject(User user, Vehicle vehicle, CreateBookingRequest request, String reason) {
        LOG.fine(() -> "Booking rejected for User#" + user.getId() + " Vehicle#" + vehicle.getId() + ": " + reason);
        audit.failure(AuditAction.BOOKING_REJECTED, user.getId(), "Vehicle#" + vehicle.getId(),
                request.startDate() + ".." + request.endDate() + " " + reason);
    }

    // ================================================================ pay

    @Override
    public Payment payForBooking(PaymentRequest request) {
        Booking booking = getBooking(request.bookingId());
        requireOwner(booking, request.userId(), AuditAction.PAYMENT);
        LOG.fine(() -> "payForBooking Booking#" + booking.getId() + " method=" + request.method());

        Payment payment = lockManager.withLock(booking.getVehicleId(), () -> {
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                audit.failure(AuditAction.PAYMENT, request.userId(), "Booking#" + booking.getId(),
                        "booking is " + booking.getStatus());
                throw new InvalidBookingStateException("Booking " + booking.getId()
                        + " is " + booking.getStatus() + ", payment not expected");
            }
            Payment p = paymentService.charge(booking.getId(), booking.getTotalAmount(),
                    request.method(), PaymentPurpose.RENTAL);
            if (p.getStatus() != PaymentStatus.SUCCESS) {
                // Booking stays PENDING_PAYMENT: the customer can retry with another method.
                audit.failure(AuditAction.PAYMENT, request.userId(), "Booking#" + booking.getId(),
                        request.method() + " Rs." + p.getAmount() + " declined: " + p.getFailureReason());
                throw new PaymentFailedException("Payment declined: " + p.getFailureReason());
            }
            booking.confirm();
            bookingRepository.save(booking);
            return p;
        });

        LOG.info(() -> "Booking#" + booking.getId() + " CONFIRMED after Payment#" + payment.getId());
        audit.success(AuditAction.PAYMENT, request.userId(), "Booking#" + booking.getId(),
                payment.getMethod() + " Rs." + payment.getAmount() + " ref=" + payment.getTransactionRef());
        publish(BookingEventType.CONFIRMED, booking);
        return payment;
    }

    // ================================================================ cancel

    @Override
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = getBooking(bookingId);
        User actor = userService.getUser(userId);
        if (!actor.isAdmin()) {
            requireOwner(booking, userId, AuditAction.BOOKING_CANCELLED);
        }

        List<Payment> refunds = lockManager.withLock(booking.getVehicleId(), () -> {
            BookingStatus before = booking.getStatus();
            try {
                booking.cancel("Cancelled by " + (actor.isAdmin() ? "admin" : "customer")); // state machine rejects ONGOING/COMPLETED
            } catch (InvalidBookingStateException e) {
                audit.failure(AuditAction.BOOKING_CANCELLED, userId, "Booking#" + bookingId, "booking is " + before);
                throw e;
            }
            List<Payment> refunded = before == BookingStatus.CONFIRMED
                    ? paymentService.refundRentalPayments(booking.getId())
                    : List.of();
            bookingRepository.save(booking);
            return refunded;
        });

        LOG.info(() -> "Booking#" + bookingId + " CANCELLED by User#" + userId + ", refunds=" + refunds.size());
        audit.success(AuditAction.BOOKING_CANCELLED, userId, "Booking#" + bookingId, booking.getCancellationReason());
        for (Payment r : refunds) {
            audit.success(AuditAction.REFUND_ISSUED, userId, "Payment#" + r.getId(),
                    "booking=" + bookingId + " Rs." + r.getAmount() + " " + r.getMethod());
        }
        publish(BookingEventType.CANCELLED, booking);
        return booking;
    }

    // ================================================================ pickup & return

    @Override
    public Booking markPickup(Long adminId, Long bookingId) {
        userService.requireAdmin(adminId);
        Booking booking = getBooking(bookingId);
        Vehicle vehicle = vehicleService.getVehicle(booking.getVehicleId());
        LocalDate today = today();

        lockManager.withLock(booking.getVehicleId(), () -> {
            // State first: a cancelled or completed booking should say so, not complain about dates.
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                audit.failure(AuditAction.VEHICLE_PICKED_UP, adminId, "Booking#" + bookingId, "booking is " + booking.getStatus());
                throw new InvalidBookingStateException("Booking " + bookingId + " is " + booking.getStatus()
                        + "; only CONFIRMED bookings can be picked up");
            }
            if (today.isBefore(booking.getStartDate())) {
                audit.failure(AuditAction.VEHICLE_PICKED_UP, adminId, "Booking#" + bookingId, "before start date " + booking.getStartDate());
                throw new InvalidRequestException("Pickup allowed from " + booking.getStartDate() + " only");
            }
            if (today.isAfter(booking.getEndDate())) {
                audit.failure(AuditAction.VEHICLE_PICKED_UP, adminId, "Booking#" + bookingId, "period ended " + booking.getEndDate());
                throw new InvalidRequestException("Booking period has already ended");
            }
            if (!vehicle.isRentable()) {
                audit.failure(AuditAction.VEHICLE_PICKED_UP, adminId, "Booking#" + bookingId, "vehicle is " + vehicle.getStatus());
                throw new VehicleNotAvailableException("Vehicle is " + vehicle.getStatus() + "; cannot hand over");
            }
            booking.startTrip(LocalDateTime.now(clock));
            return bookingRepository.save(booking);
        });

        LOG.info(() -> "Booking#" + bookingId + " picked up: Vehicle#" + vehicle.getId() + " handed over at "
                + booking.getActualPickupTime());
        audit.success(AuditAction.VEHICLE_PICKED_UP, adminId, "Booking#" + bookingId,
                "vehicle=" + vehicle.getId() + " at " + booking.getActualPickupTime());
        publish(BookingEventType.PICKED_UP, booking);
        return booking;
    }

    @Override
    public Booking markReturn(Long adminId, Long bookingId, LocalDateTime returnTime, PaymentMethod lateFeeMethod) {
        userService.requireAdmin(adminId);
        Booking booking = getBooking(bookingId);
        Vehicle vehicle = vehicleService.getVehicle(booking.getVehicleId());
        if (returnTime == null) {
            throw new InvalidRequestException("returnTime must not be null");
        }

        lockManager.withLock(booking.getVehicleId(), () -> {
            if (booking.getStatus() != BookingStatus.ONGOING) {
                audit.failure(AuditAction.VEHICLE_RETURNED, adminId, "Booking#" + bookingId, "booking is " + booking.getStatus());
                throw new InvalidBookingStateException("Booking " + bookingId + " is " + booking.getStatus()
                        + "; only ONGOING bookings can be returned");
            }
            if (returnTime.isBefore(booking.getActualPickupTime())) {
                throw new InvalidRequestException("Return time cannot be before pickup time");
            }
            BigDecimal lateFee = lateFeePolicy.calculateLateFee(vehicle, booking.getEndDate(), returnTime);
            LOG.fine(() -> "Late fee for Booking#" + bookingId + ": due " + booking.getEndDate()
                    + ", returned " + returnTime + " -> Rs." + lateFee);
            if (lateFee.signum() > 0) {
                PaymentMethod method = lateFeeMethod != null ? lateFeeMethod : PaymentMethod.CASH;
                Payment fee = paymentService.charge(booking.getId(), lateFee, method, PaymentPurpose.LATE_FEE);
                if (fee.getStatus() != PaymentStatus.SUCCESS) {
                    audit.failure(AuditAction.LATE_FEE_CHARGED, adminId, "Booking#" + bookingId,
                            method + " Rs." + lateFee + " declined: " + fee.getFailureReason());
                    throw new PaymentFailedException("Late fee payment declined: " + fee.getFailureReason());
                }
                audit.success(AuditAction.LATE_FEE_CHARGED, adminId, "Booking#" + bookingId,
                        method + " Rs." + lateFee + " ref=" + fee.getTransactionRef());
            }
            booking.completeTrip(returnTime, lateFee);
            return bookingRepository.save(booking);
        });

        LOG.info(() -> "Booking#" + bookingId + " COMPLETED: returned " + returnTime + ", total Rs."
                + booking.getTotalAmount() + " (late fee Rs." + booking.getLateFee() + ")");
        audit.success(AuditAction.VEHICLE_RETURNED, adminId, "Booking#" + bookingId,
                "vehicle=" + vehicle.getId() + " at " + returnTime + " total=" + booking.getTotalAmount());
        publish(BookingEventType.RETURNED, booking);
        return booking;
    }

    // ================================================================ queries

    @Override
    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    @Override
    public List<Booking> getUserBookings(Long userId) {
        userService.getUser(userId);
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Payment> getPayments(Long bookingId) {
        getBooking(bookingId);
        return paymentService.getPaymentsForBooking(bookingId);
    }

    // ================================================================ housekeeping

    @Override
    public int expireUnpaidBookings(Duration paymentWindow) {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(paymentWindow);
        List<Booking> candidates = bookingRepository.findByStatusCreatedBefore(BookingStatus.PENDING_PAYMENT, cutoff);
        LOG.fine(() -> "Expiry sweep: " + candidates.size() + " unpaid booking(s) created before " + cutoff);
        int expired = 0;
        for (Booking candidate : candidates) {
            boolean changed = lockManager.withLock(candidate.getVehicleId(), () -> {
                // Re-check under the lock: the customer may have paid a moment ago.
                if (candidate.getStatus() != BookingStatus.PENDING_PAYMENT) {
                    LOG.fine(() -> "Booking#" + candidate.getId() + " was paid meanwhile, not expiring");
                    return false;
                }
                candidate.cancel("Payment not received within " + paymentWindow.toMinutes() + " minutes");
                bookingRepository.save(candidate);
                return true;
            });
            if (changed) {
                expired++;
                LOG.info(() -> "Booking#" + candidate.getId() + " EXPIRED (unpaid), Vehicle#"
                        + candidate.getVehicleId() + " released");
                audit.success(AuditAction.BOOKING_EXPIRED, null, "Booking#" + candidate.getId(),
                        "unpaid after " + paymentWindow.toMinutes() + " min, vehicle=" + candidate.getVehicleId());
                publish(BookingEventType.EXPIRED, candidate);
            }
        }
        return expired;
    }

    @Override
    public void addListener(BookingEventListener listener) {
        listeners.add(listener);
    }

    // ================================================================ helpers

    private void requireOwner(Booking booking, Long userId, AuditAction action) {
        if (!booking.getUserId().equals(userId)) {
            audit.denied(action, userId, "Booking#" + booking.getId(), "not the owner (owner=" + booking.getUserId() + ")");
            throw new AccessDeniedException("Booking " + booking.getId() + " does not belong to user " + userId);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Observer: tell every listener. A failing listener must never break the booking flow. */
    private void publish(BookingEventType type, Booking booking) {
        BookingEvent event = new BookingEvent(type, booking);
        LOG.finest(() -> "Publishing " + type + " for Booking#" + booking.getId() + " to " + listeners.size() + " listener(s)");
        for (BookingEventListener listener : listeners) {
            try {
                listener.onBookingEvent(event);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Listener " + listener.getClass().getSimpleName() + " failed for "
                        + type + " on Booking#" + booking.getId(), e);
            }
        }
    }
}
