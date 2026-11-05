package com.jyoti.carrental.model.enums;

/**
 * Lifecycle of a booking, with the allowed transitions built into the enum
 * (a light form of the <b>State pattern</b>).
 *
 * <pre>
 *  PENDING_PAYMENT ──► CONFIRMED ──► ONGOING ──► COMPLETED
 *        │                 │
 *        ▼                 ▼
 *    CANCELLED         CANCELLED (+ refund)
 * </pre>
 *
 * Keeping the rules here means no service can accidentally move a booking
 * from COMPLETED back to ONGOING, for example.
 *
 * @author Jyoti Prakash Parida
 */
public enum BookingStatus {
    /** Created, waiting for payment. Holds the vehicle for a short time only. */
    PENDING_PAYMENT,
    /** Paid. Vehicle reserved for the dates. */
    CONFIRMED,
    /** Vehicle handed over to the customer. */
    ONGOING,
    /** Vehicle returned. Final state. */
    COMPLETED,
    /** Cancelled by customer or expired because payment never arrived. Final state. */
    CANCELLED;

    /**
     * @param next the status we want to move to
     * @return true if moving from this status to {@code next} is a legal transition
     */
    public boolean canTransitionTo(BookingStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == ONGOING || next == CANCELLED;
            case ONGOING -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false; // final states
        };
    }

    /**
     * A booking in one of these states occupies the vehicle for its dates,
     * so no other booking may overlap it.
     *
     * @return true if this status blocks the vehicle's inventory
     */
    public boolean blocksInventory() {
        return this == PENDING_PAYMENT || this == CONFIRMED || this == ONGOING;
    }
}
