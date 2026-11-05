package com.jyoti.carrental.dto.request;

import com.jyoti.carrental.util.Validate;

import java.time.LocalDate;

/**
 * Input for creating a booking. Date rules that need "today" (no past dates, max length)
 * are checked in the service, because the DTO does not know the clock.
 *
 * @author Jyoti Prakash Parida
 */
public record CreateBookingRequest(Long userId, Long vehicleId, LocalDate startDate, LocalDate endDate) {

    public CreateBookingRequest {
        Validate.notNull(userId, "userId");
        Validate.notNull(vehicleId, "vehicleId");
        Validate.notNull(startDate, "startDate");
        Validate.notNull(endDate, "endDate");
    }
}
