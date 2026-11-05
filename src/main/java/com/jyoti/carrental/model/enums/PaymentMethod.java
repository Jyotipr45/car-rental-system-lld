package com.jyoti.carrental.model.enums;

/**
 * How the customer pays. Each value has exactly one
 * {@link com.jyoti.carrental.payment.PaymentProcessor} implementation,
 * selected at runtime by {@link com.jyoti.carrental.payment.PaymentProcessorFactory}.
 *
 * @author Jyoti Prakash Parida
 */
public enum PaymentMethod {
    CARD,
    UPI,
    CASH
}
