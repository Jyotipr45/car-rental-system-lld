package com.jyoti.carrental.exception;

/**
 * Thrown when a user, branch, vehicle, booking or payment id does not exist.
 * Mapped to HTTP 404 by {@link com.jyoti.carrental.controller.GlobalExceptionHandler}.
 *
 * @author Jyoti Prakash Parida
 */
public class ResourceNotFoundException extends CarRentalException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message, 404);
    }
}
