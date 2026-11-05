package com.jyoti.carrental.exception;

/**
 * Thrown when the payment gateway declines a payment.
 * Mapped to HTTP 402 by {@link com.jyoti.carrental.controller.GlobalExceptionHandler}.
 *
 * @author Jyoti Prakash Parida
 */
public class PaymentFailedException extends CarRentalException {

    private static final long serialVersionUID = 1L;

    public PaymentFailedException(String message) {
        super(message, 402);
    }
}
