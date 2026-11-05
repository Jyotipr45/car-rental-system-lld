package com.jyoti.carrental.exception;

/**
 * Thrown when the vehicle is already booked for overlapping dates or is out of service.
 * Mapped to HTTP 409 by {@link com.jyoti.carrental.controller.GlobalExceptionHandler}.
 *
 * @author Jyoti Prakash Parida
 */
public class VehicleNotAvailableException extends CarRentalException {

    private static final long serialVersionUID = 1L;

    public VehicleNotAvailableException(String message) {
        super(message, 409);
    }
}
