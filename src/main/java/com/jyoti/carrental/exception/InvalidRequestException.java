package com.jyoti.carrental.exception;

/**
 * Thrown when input is missing or breaks a business rule (bad dates, blank fields).
 * Mapped to HTTP 400 by {@link com.jyoti.carrental.controller.GlobalExceptionHandler}.
 *
 * @author Jyoti Prakash Parida
 */
public class InvalidRequestException extends CarRentalException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message, 400);
    }
}
