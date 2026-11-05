package com.jyoti.carrental.exception;

/**
 * Thrown when a unique value (email, registration number) already exists.
 * Mapped to HTTP 409 by {@link com.jyoti.carrental.controller.GlobalExceptionHandler}.
 *
 * @author Jyoti Prakash Parida
 */
public class DuplicateResourceException extends CarRentalException {

    private static final long serialVersionUID = 1L;

    public DuplicateResourceException(String message) {
        super(message, 409);
    }
}
