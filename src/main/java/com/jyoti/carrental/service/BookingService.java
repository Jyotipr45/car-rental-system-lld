package com.jyoti.carrental.service;

import com.jyoti.carrental.dto.request.CreateBookingRequest;
import com.jyoti.carrental.dto.request.PaymentRequest;
import com.jyoti.carrental.event.BookingEventListener;
import com.jyoti.carrental.model.Booking;
import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.enums.PaymentMethod;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The heart of the system: the full booking lifecycle.
 *
 * <pre>
 * createBooking → payForBooking → markPickup → markReturn
 *        └──────── cancelBooking / expireUnpaidBookings
 * </pre>
 *
 * @author Jyoti Prakash Parida
 */
public interface BookingService {

    /** Validates, re-checks availability under a lock, prices and saves (PENDING_PAYMENT). */
    Booking createBooking(CreateBookingRequest request);

    /** Charges the booking total; on success the booking becomes CONFIRMED. */
    Payment payForBooking(PaymentRequest request);

    /** Customer cancels before pickup; refunds if already paid. */
    Booking cancelBooking(Long bookingId, Long userId);

    /** Admin hands over the vehicle: CONFIRMED → ONGOING. */
    Booking markPickup(Long adminId, Long bookingId);

    /** Admin receives the vehicle: ONGOING → COMPLETED, charging any late fee. */
    Booking markReturn(Long adminId, Long bookingId, LocalDateTime returnTime, PaymentMethod lateFeeMethod);

    Booking getBooking(Long bookingId);

    List<Booking> getUserBookings(Long userId);

    List<Payment> getPayments(Long bookingId);

    /**
     * Cancels PENDING_PAYMENT bookings older than {@code paymentWindow}, so unpaid
     * bookings don't hold vehicles forever.
     *
     * @return how many bookings were expired
     */
    int expireUnpaidBookings(Duration paymentWindow);

    /** Observer registration. */
    void addListener(BookingEventListener listener);
}
