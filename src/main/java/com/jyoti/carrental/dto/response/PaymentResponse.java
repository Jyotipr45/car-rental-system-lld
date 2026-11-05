package com.jyoti.carrental.dto.response;

import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.PaymentPurpose;
import com.jyoti.carrental.model.enums.PaymentStatus;

import java.math.BigDecimal;

/**
 * Public view of a payment.
 *
 * @author Jyoti Prakash Parida
 */
public record PaymentResponse(Long id, Long bookingId, BigDecimal amount, PaymentMethod method,
                              PaymentPurpose purpose, PaymentStatus status, String transactionRef) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getBookingId(), p.getAmount(), p.getMethod(),
                p.getPurpose(), p.getStatus(), p.getTransactionRef());
    }

    @Override
    public String toString() {
        return "Payment#" + id + " Rs." + amount + " " + method + " " + purpose + " [" + status + "]"
                + (transactionRef != null ? " ref=" + transactionRef : "");
    }
}
