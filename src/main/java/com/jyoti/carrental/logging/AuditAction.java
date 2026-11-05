package com.jyoti.carrental.logging;

/**
 * Every business action that must leave an audit trail.
 *
 * <p>Audit answers "who did what, to which record, when, and did it work?". It is kept
 * separate from debug logging because it has a different reader (support, finance,
 * security) and a different retention period.</p>
 *
 * @author Jyoti Prakash Parida
 */
public enum AuditAction {
    // identity & access
    USER_REGISTERED,
    LOGIN,
    ACCESS_DENIED,
    // inventory
    BRANCH_CREATED,
    VEHICLE_ADDED,
    VEHICLE_STATUS_CHANGED,
    // booking lifecycle
    BOOKING_CREATED,
    BOOKING_REJECTED,
    PAYMENT,
    BOOKING_CANCELLED,
    REFUND_ISSUED,
    VEHICLE_PICKED_UP,
    VEHICLE_RETURNED,
    LATE_FEE_CHARGED,
    BOOKING_EXPIRED
}
