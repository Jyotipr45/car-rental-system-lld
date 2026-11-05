package com.jyoti.carrental.dto.response;

import com.jyoti.carrental.model.User;
import com.jyoti.carrental.model.enums.UserRole;

/**
 * Public view of a user. Note: no password hash leaves the service layer.
 *
 * @author Jyoti Prakash Parida
 */
public record UserResponse(Long id, String name, String email, UserRole role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
