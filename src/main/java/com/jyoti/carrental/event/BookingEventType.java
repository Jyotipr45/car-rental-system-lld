package com.jyoti.carrental.event;

/**
 * Things that happen to a booking that other parts of the system may care about.
 *
 * @author Jyoti Prakash Parida
 */
public enum BookingEventType {
    CREATED,
    CONFIRMED,
    PICKED_UP,
    RETURNED,
    CANCELLED,
    EXPIRED
}
