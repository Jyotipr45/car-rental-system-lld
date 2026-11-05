package com.jyoti.carrental.exception;

/**
 * Thrown when a user tries an action their role does not allow (e.g. a customer adding a car).
 * Mapped to HTTP 403 by {@link com.jyoti.carrental.controller.GlobalExceptionHandler}.
 *
 * @author Jyoti Prakash Parida
 */
public class AccessDeniedException extends CarRentalException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message, 403);
    }
}
