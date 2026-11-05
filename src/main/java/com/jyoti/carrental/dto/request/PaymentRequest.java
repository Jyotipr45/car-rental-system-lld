package com.jyoti.carrental.dto.request;

import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.util.Validate;

/**
 * Input for paying a booking.
 *
 * @author Jyoti Prakash Parida
 */
public record PaymentRequest(Long bookingId, Long userId, PaymentMethod method) {

    public PaymentRequest {
        Validate.notNull(bookingId, "bookingId");
        Validate.notNull(userId, "userId");
        Validate.notNull(method, "method");
    }
}
