package com.jyoti.carrental.model.enums;

/**
 * Role of a user in the system.
 *
 * <p>Why an enum? The set of roles is fixed in code and drives permission checks.
 * If roles had to be created by admins at runtime, this would be a database table instead.</p>
 *
 * @author Jyoti Prakash Parida
 */
public enum UserRole {
    /** Rents vehicles: searches, books, pays, cancels. */
    CUSTOMER,
    /** Runs a branch: manages inventory, hands over and receives vehicles. */
    ADMIN
}
