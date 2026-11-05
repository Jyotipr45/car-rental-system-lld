package com.jyoti.carrental.exception;

/**
 * Thrown when a booking status change breaks the state machine (e.g. pickup a cancelled booking).
 * Mapped to HTTP 409 by {@link com.jyoti.carrental.controller.GlobalExceptionHandler}.
 *
 * @author Jyoti Prakash Parida
 */
public class InvalidBookingStateException extends CarRentalException {

    private static final long serialVersionUID = 1L;

    public InvalidBookingStateException(String message) {
        super(message, 409);
    }
}
