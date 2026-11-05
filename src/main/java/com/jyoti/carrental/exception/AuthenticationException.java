package com.jyoti.carrental.exception;

/**
 * Thrown when login credentials are wrong.
 * Mapped to HTTP 401 by {@link com.jyoti.carrental.controller.GlobalExceptionHandler}.
 *
 * @author Jyoti Prakash Parida
 */
public class AuthenticationException extends CarRentalException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message, 401);
    }
}
