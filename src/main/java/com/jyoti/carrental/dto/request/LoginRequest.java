package com.jyoti.carrental.dto.request;

import com.jyoti.carrental.util.Validate;

/**
 * Input for login.
 *
 * @author Jyoti Prakash Parida
 */
public record LoginRequest(String email, String password) {

    public LoginRequest {
        email = Validate.email(email);
        Validate.notBlank(password, "password");
    }
}
