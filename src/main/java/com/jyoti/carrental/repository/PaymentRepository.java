package com.jyoti.carrental.repository;

import com.jyoti.carrental.model.Payment;

import java.time.Clock;
import java.util.List;

/**
 * Data access for {@link Payment}.
 *
 * @author Jyoti Prakash Parida
 */
public class PaymentRepository extends InMemoryRepository<Payment> {

    public PaymentRepository(Clock clock) {
        super(clock);
    }

    public List<Payment> findByBookingId(Long bookingId) {
        return findWhere(p -> p.getBookingId().equals(bookingId));
    }
}
