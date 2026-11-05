package com.jyoti.carrental.model;

import com.jyoti.carrental.model.enums.UserRole;

/**
 * A person who uses the system: either a customer or an admin.
 *
 * <p><b>Design choice:</b> Customer and Admin differ only in permissions, so one
 * concrete class plus a {@link UserRole} enum is enough. If they later need different
 * fields (e.g. admin {@code employeeId}), split into {@code abstract Person → Customer, Admin}.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class User extends BaseEntity {

    private String name;
    /** Unique login id. */
    private String email;
    private String phone;
    /** Never store raw passwords. See {@link com.jyoti.carrental.util.PasswordHasher}. */
    private String passwordHash;
    /** Mandatory for customers, because only licensed drivers may rent. */
    private String drivingLicenseNo;
    private UserRole role;

    public User(String name, String email, String phone, String passwordHash,
                String drivingLicenseNo, UserRole role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.drivingLicenseNo = drivingLicenseNo;
        this.role = role;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getDrivingLicenseNo() { return drivingLicenseNo; }
    public UserRole getRole() { return role; }

    @Override
    public String toString() {
        return "User{id=" + getId() + ", name='" + name + "', role=" + role + "}";
    }
}
