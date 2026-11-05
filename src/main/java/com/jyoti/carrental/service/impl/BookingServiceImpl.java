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
 * @author Jyoti Prakash Parida
 */
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final PaymentService paymentService;
    private final PricingStrategy pricingStrategy;
    private final LateFeePolicy lateFeePolicy;
    private final VehicleLockManager lockManager;
    private final Clock clock;
    /** Thread-safe list because listeners may be added while events are published. */
    private final List<BookingEventListener> listeners = new CopyOnWriteArrayList<>();

    public BookingServiceImpl(BookingRepository bookingRepository, UserService userService,
                              VehicleService vehicleService, PaymentService paymentService,
                              PricingStrategy pricingStrategy, LateFeePolicy lateFeePolicy,
                              VehicleLockManager lockManager, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.paymentService = paymentService;
        this.pricingStrategy = pricingStrategy;
        this.lateFeePolicy = lateFeePolicy;
        this.lockManager = lockManager;
        this.clock = clock;
    }

    // ================================================================ create

    @Override
    public Booking createBooking(CreateBookingRequest request) {
        User user = userService.getUser(request.userId());
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new AccessDeniedException("Only customers can create bookings");
        }
        Vehicle vehicle = vehicleService.getVehicle(request.vehicleId());
        BookingRules.validateDates(request.startDate(), request.endDate(), today());

        // Critical section: availability check + save must be atomic per vehicle.
        Booking saved = lockManager.withLock(vehicle.getId(), () -> {
            if (!vehicle.isRentable()) {
                throw new VehicleNotAvailableException("Vehicle " + vehicle.getId() + " is " + vehicle.getStatus());
            }
            // Always re-check here: the search result the customer saw may already be stale.
            if (bookingRepository.existsActiveOverlap(vehicle.getId(), request.startDate(), request.endDate())) {
                throw new VehicleNotAvailableException("Vehicle " + vehicle.getId()
                        + " is already booked between " + request.startDate() + " and " + request.endDate());
            }
            BigDecimal price = pricingStrategy.calculatePrice(vehicle, request.startDate(), request.endDate());
            Booking booking = new Booking(user.getId(), vehicle.getId(), vehicle.getBranchId(),
                    request.startDate(), request.endDate(), price);
            return bookingRepository.save(booking);
        });

        publish(BookingEventType.CREATED, saved);
        return saved;
    }

    // ================================================================ pay

    @Override
    public Payment payForBooking(PaymentRequest request) {
        Booking booking = getBooking(request.bookingId());
        requireOwner(booking, request.userId());

        Payment payment = lockManager.withLock(booking.getVehicleId(), () -> {
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                throw new InvalidBookingStateException("Booking " + booking.getId()
                        + " is " + booking.getStatus() + ", payment not expected");
            }
            Payment p = paymentService.charge(booking.getId(), booking.getTotalAmount(),
                    request.method(), PaymentPurpose.RENTAL);
            if (p.getStatus() != PaymentStatus.SUCCESS) {
                // Booking stays PENDING_PAYMENT: the customer can retry with another method.
                throw new PaymentFailedException("Payment declined: " + p.getFailureReason());
            }
            booking.confirm();
            bookingRepository.save(booking);
            return p;
        });

        publish(BookingEventType.CONFIRMED, booking);
        return payment;
    }

    // ================================================================ cancel

    @Override
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = getBooking(bookingId);
        User actor = userService.getUser(userId);
        if (!actor.isAdmin()) {
            requireOwner(booking, userId);
        }

        lockManager.withLock(booking.getVehicleId(), () -> {
            boolean wasPaid = booking.getStatus() == BookingStatus.CONFIRMED;
            booking.cancel("Cancelled by " + (actor.isAdmin() ? "admin" : "customer")); // state machine rejects ONGOING/COMPLETED
            if (wasPaid) {
                paymentService.refundRentalPayments(booking.getId());
            }
            return bookingRepository.save(booking);
        });

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
                throw new InvalidBookingStateException("Booking " + bookingId + " is " + booking.getStatus()
                        + "; only CONFIRMED bookings can be picked up");
            }
            if (today.isBefore(booking.getStartDate())) {
                throw new InvalidRequestException("Pickup allowed from " + booking.getStartDate() + " only");
            }
            if (today.isAfter(booking.getEndDate())) {
                throw new InvalidRequestException("Booking period has already ended");
            }
            if (!vehicle.isRentable()) {
                throw new VehicleNotAvailableException("Vehicle is " + vehicle.getStatus() + "; cannot hand over");
            }
            booking.startTrip(LocalDateTime.now(clock));
            return bookingRepository.save(booking);
        });

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
                throw new InvalidBookingStateException("Booking " + bookingId + " is " + booking.getStatus()
                        + "; only ONGOING bookings can be returned");
            }
            if (returnTime.isBefore(booking.getActualPickupTime())) {
                throw new InvalidRequestException("Return time cannot be before pickup time");
            }
            BigDecimal lateFee = lateFeePolicy.calculateLateFee(vehicle, booking.getEndDate(), returnTime);
            if (lateFee.signum() > 0) {
                PaymentMethod method = lateFeeMethod != null ? lateFeeMethod : PaymentMethod.CASH;
                Payment fee = paymentService.charge(booking.getId(), lateFee, method, PaymentPurpose.LATE_FEE);
                if (fee.getStatus() != PaymentStatus.SUCCESS) {
                    throw new PaymentFailedException("Late fee payment declined: " + fee.getFailureReason());
                }
            }
            booking.completeTrip(returnTime, lateFee);
            return bookingRepository.save(booking);
        });

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
        int expired = 0;
        for (Booking candidate : bookingRepository.findByStatusCreatedBefore(BookingStatus.PENDING_PAYMENT, cutoff)) {
            boolean changed = lockManager.withLock(candidate.getVehicleId(), () -> {
                // Re-check under the lock: the customer may have paid a moment ago.
                if (candidate.getStatus() != BookingStatus.PENDING_PAYMENT) {
                    return false;
                }
                candidate.cancel("Payment not received within " + paymentWindow.toMinutes() + " minutes");
                bookingRepository.save(candidate);
                return true;
            });
            if (changed) {
                expired++;
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

    private void requireOwner(Booking booking, Long userId) {
        if (!booking.getUserId().equals(userId)) {
            throw new AccessDeniedException("Booking " + booking.getId() + " does not belong to user " + userId);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Observer: tell every listener. A failing listener must never break the booking flow. */
    private void publish(BookingEventType type, Booking booking) {
        BookingEvent event = new BookingEvent(type, booking);
        for (BookingEventListener listener : listeners) {
            try {
                listener.onBookingEvent(event);
            } catch (RuntimeException e) {
                System.err.println("Listener failed for " + type + ": " + e.getMessage());
            }
        }
    }
}
