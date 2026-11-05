package com.jyoti.carrental.dto.request;

import com.jyoti.carrental.model.enums.UserRole;
import com.jyoti.carrental.util.Validate;

/**
 * Input for user registration.
 *
 * <p><b>Why DTOs?</b> Controllers never accept or return entities directly. A DTO exposes
 * only the fields the API needs (no id, no passwordHash) and validates itself, so the
 * entity can change without breaking clients.</p>
 *
 * @author Jyoti Prakash Parida
 */
public record RegisterUserRequest(String name, String email, String phone, String password,
                                  String drivingLicenseNo, UserRole role) {

    /** Compact constructor = validation on creation (like {@code @Valid}). */
    public RegisterUserRequest {
        name = Validate.notBlank(name, "name");
        email = Validate.email(email);
        phone = Validate.notBlank(phone, "phone");
        Validate.minLength(password, 6, "password");
        Validate.notNull(role, "role");
        if (role == UserRole.CUSTOMER) {
            drivingLicenseNo = Validate.notBlank(drivingLicenseNo, "drivingLicenseNo");
        }
    }
}
