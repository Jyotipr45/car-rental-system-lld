package com.jyoti.carrental.event;

import com.jyoti.carrental.model.Booking;

/**
 * Stand-in for an email/SMS sender: prints a customer-facing message to the console.
 *
 * @author Jyoti Prakash Parida
 */
public class ConsoleNotificationListener implements BookingEventListener {

    private final boolean enabled;

    public ConsoleNotificationListener(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onBookingEvent(BookingEvent event) {
        if (!enabled) {
            return;
        }
        Booking b = event.booking();
        String text = switch (event.type()) {
            case CREATED -> "Booking #" + b.getId() + " created. Please pay Rs." + b.getTotalAmount() + " to confirm.";
            case CONFIRMED -> "Booking #" + b.getId() + " confirmed for " + b.getStartDate() + " -> " + b.getEndDate() + ".";
            case PICKED_UP -> "Enjoy your trip! Booking #" + b.getId() + " is now ongoing.";
            case RETURNED -> "Thanks for returning the vehicle. Final amount Rs." + b.getTotalAmount()
                    + (b.getLateFee().signum() > 0 ? " (incl. late fee Rs." + b.getLateFee() + ")" : "") + ".";
            case CANCELLED -> "Booking #" + b.getId() + " cancelled: " + b.getCancellationReason();
            case EXPIRED -> "Booking #" + b.getId() + " expired because payment was not received.";
        };
        System.out.println("    [notify user " + b.getUserId() + "] " + text);
    }
}
