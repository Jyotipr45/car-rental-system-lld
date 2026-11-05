package com.jyoti.carrental.event;

import com.jyoti.carrental.model.Booking;

/**
 * Message published when a booking changes.
 *
 * @param type    what happened
 * @param booking the booking after the change
 * @author Jyoti Prakash Parida
 */
public record BookingEvent(BookingEventType type, Booking booking) {
}
