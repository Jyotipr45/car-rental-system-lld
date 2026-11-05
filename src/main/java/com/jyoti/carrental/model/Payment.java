package com.jyoti.carrental.model;

import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.PaymentPurpose;
import com.jyoti.carrental.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single money movement for a booking.
 *
 * <p>Relationship: Booking 1 ──── N Payment (rental payment, then maybe a late-fee payment).
 * The foreign key {@code bookingId} is on this, the "many" side.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class Payment extends BaseEntity {

    private final Long bookingId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private final PaymentPurpose purpose;
    private PaymentStatus status = PaymentStatus.PENDING;
    /** Reference returned by the (mock) gateway, used for refunds and reconciliation. */
    private String transactionRef;
    private String failureReason;
    private LocalDateTime paidAt;

    public Payment(Long bookingId, BigDecimal amount, PaymentMethod method, PaymentPurpose purpose) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.purpose = purpose;
    }

    public void markSuccess(String transactionRef, LocalDateTime paidAt) {
        this.status = PaymentStatus.SUCCESS;
        this.transactionRef = transactionRef;
        this.paidAt = paidAt;
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    public Long getBookingId() { return bookingId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentPurpose getPurpose() { return purpose; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionRef() { return transactionRef; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getPaidAt() { return paidAt; }

    @Override
    public String toString() {
        return "Payment{id=" + getId() + ", booking=" + bookingId + ", Rs." + amount + ", " + method
                + ", " + purpose + ", " + status + (transactionRef != null ? ", ref=" + transactionRef : "") + "}";
    }
}
