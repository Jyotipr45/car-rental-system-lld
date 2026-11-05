package com.jyoti.carrental.model.enums;

/**
 * Whether a vehicle can be rented <b>at all</b>.
 *
 * <p><b>Design note:</b> there is deliberately no {@code BOOKED} or {@code RENTED} value.
 * A vehicle can be booked for 10–12 Nov and still be free on 13 Nov, so availability
 * depends on dates. It is calculated from bookings (see
 * {@link com.jyoti.carrental.repository.BookingRepository#existsActiveOverlap}),
 * never stored as a flag on the vehicle.</p>
 *
 * @author Jyoti Prakash Parida
 */
public enum VehicleStatus {
    /** Usable and can appear in search results. */
    AVAILABLE,
    /** Temporarily out of service (servicing, repair). Hidden from search. */
    UNDER_MAINTENANCE,
    /** Retired from the fleet. Hidden from search. */
    INACTIVE
}
