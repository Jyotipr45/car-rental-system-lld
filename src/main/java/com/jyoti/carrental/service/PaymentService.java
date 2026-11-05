package com.jyoti.carrental.service;

import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.PaymentPurpose;

import java.math.BigDecimal;
import java.util.List;

/**
 * Moves money. Deliberately knows <b>nothing about booking states</b>.
 *
 * <p><b>Design decision:</b> if PaymentService confirmed bookings and BookingService
 * refunded payments, the two would depend on each other (a circular dependency).
 * Instead BookingService is the orchestrator: it calls PaymentService and then updates
 * the booking itself. Dependencies point one way only.</p>
 *
 * @author Jyoti Prakash Parida
 */
public interface PaymentService {

    /**
     * Attempts a charge and always records the attempt (success or failure) for audit.
     *
     * @return the saved payment, with status SUCCESS or FAILED
     */
    Payment charge(Long bookingId, BigDecimal amount, PaymentMethod method, PaymentPurpose purpose);

    /** Refunds every successful RENTAL payment of the booking. */
    List<Payment> refundRentalPayments(Long bookingId);

    List<Payment> getPaymentsForBooking(Long bookingId);
}
