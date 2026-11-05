package com.jyoti.carrental.exception;

/**
 * Base class for every business error in the application.
 *
 * <p>Unchecked (extends RuntimeException) so service signatures stay clean, the same
 * convention Spring uses. Each subclass carries the HTTP status it maps to, so the
 * {@link com.jyoti.carrental.controller.GlobalExceptionHandler} needs no if-else chain.</p>
 *
 * @author Jyoti Prakash Parida
 */
public abstract class CarRentalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;

    protected CarRentalException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
