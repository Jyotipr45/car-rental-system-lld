package com.jyoti.carrental.payment;

/**
 * Immutable outcome of a gateway call. A Java {@code record} gives us constructor,
 * getters, equals/hashCode and toString for free.
 *
 * @param success        true if the gateway accepted the payment
 * @param transactionRef gateway reference (null on failure)
 * @param message        human-readable detail, e.g. the decline reason
 * @author Jyoti Prakash Parida
 */
public record PaymentResult(boolean success, String transactionRef, String message) {

    public static PaymentResult success(String transactionRef) {
        return new PaymentResult(true, transactionRef, "OK");
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
