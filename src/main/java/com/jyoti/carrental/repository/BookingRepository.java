package com.jyoti.carrental.repository;

import com.jyoti.carrental.model.Booking;
import com.jyoti.carrental.model.enums.BookingStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access for {@link Booking}.
 *
 * @author Jyoti Prakash Parida
 */
public class BookingRepository extends InMemoryRepository<Booking> {

    public BookingRepository(Clock clock) {
        super(clock);
    }

    public List<Booking> findByUserId(Long userId) {
        return findWhere(b -> b.getUserId().equals(userId));
    }

    /**
     * <b>The most important query in the system.</b> Is this vehicle already held by
     * another active booking on any of these dates? SQL equivalent:
     *
     * <pre>
     * SELECT COUNT(*) FROM booking b
     *  WHERE b.vehicle_id = :vehicleId
     *    AND b.status IN ('PENDING_PAYMENT','CONFIRMED','ONGOING')
     *    AND b.start_date &lt;= :newEnd
     *    AND b.end_date   &gt;= :newStart
     * </pre>
     */
    public boolean existsActiveOverlap(Long vehicleId, LocalDate start, LocalDate end) {
        return !findWhere(b -> b.getVehicleId().equals(vehicleId)
                && b.blocksInventory()
                && b.overlaps(start, end)).isEmpty();
    }

    /** Used by the cleanup job to find unpaid bookings that are too old. */
    public List<Booking> findByStatusCreatedBefore(BookingStatus status, LocalDateTime cutoff) {
        return findWhere(b -> b.getStatus() == status && !b.getCreatedAt().isAfter(cutoff));
    }
}
