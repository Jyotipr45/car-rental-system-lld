package com.jyoti.carrental.logging;

/**
 * Result of an audited action.
 *
 * @author Jyoti Prakash Parida
 */
public enum AuditOutcome {
    /** The action completed. */
    SUCCESS,
    /** The action was attempted but failed a business rule or a payment was declined. */
    FAILURE,
    /** The user was not allowed to perform the action. */
    DENIED
}
