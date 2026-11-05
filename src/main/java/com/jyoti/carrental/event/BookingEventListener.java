package com.jyoti.carrental.event;

/**
 * <b>Observer pattern.</b> Anything that wants to react to booking changes (email/SMS,
 * analytics, audit log) implements this and registers with the booking service.
 *
 * <p>The booking service does not know who is listening, so adding an SMS sender later
 * needs zero changes in booking code. In Spring this maps to
 * {@code ApplicationEventPublisher} + {@code @EventListener}; in microservices, to a
 * Kafka topic.</p>
 *
 * @author Jyoti Prakash Parida
 */
@FunctionalInterface
public interface BookingEventListener {

    void onBookingEvent(BookingEvent event);
}
