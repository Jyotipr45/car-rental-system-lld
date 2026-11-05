package com.jyoti.carrental.dto.response;

import com.jyoti.carrental.model.Booking;
import com.jyoti.carrental.model.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Public view of a booking.
 *
 * @author Jyoti Prakash Parida
 */
public record BookingResponse(Long id, Long userId, Long vehicleId, LocalDate startDate, LocalDate endDate,
                              BigDecimal baseAmount, BigDecimal lateFee, BigDecimal totalAmount,
                              BookingStatus status) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getUserId(), b.getVehicleId(), b.getStartDate(), b.getEndDate(),
                b.getBaseAmount(), b.getLateFee(), b.getTotalAmount(), b.getStatus());
    }

    @Override
    public String toString() {
        return "Booking#" + id + " vehicle#" + vehicleId + " " + startDate + "->" + endDate
                + " total Rs." + totalAmount + (lateFee.signum() > 0 ? " (late fee Rs." + lateFee + ")" : "")
                + " [" + status + "]";
    }
}
