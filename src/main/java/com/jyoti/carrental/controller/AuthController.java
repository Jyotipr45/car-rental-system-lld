package com.jyoti.carrental.controller;

import com.jyoti.carrental.dto.request.LoginRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.dto.response.UserResponse;
import com.jyoti.carrental.service.UserService;

import java.util.function.Supplier;

/**
 * Registration and login endpoints.
 *
 * <p><b>Controller rule:</b> no business logic here. Translate input → call service →
 * translate output. Each method notes the REST endpoint it would be in Spring.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class AuthController {

    private final UserService userService;
    private final GlobalExceptionHandler handler;

    public AuthController(UserService userService, GlobalExceptionHandler handler) {
        this.userService = userService;
        this.handler = handler;
    }

    /** POST /api/v1/auth/register */
    public ApiResponse<UserResponse> register(Supplier<RegisterUserRequest> request) {
        // The request is built inside the handler so validation errors become 400 responses.
        return handler.handle(201, () -> UserResponse.from(userService.register(request.get())));
    }

    /** POST /api/v1/auth/login */
    public ApiResponse<UserResponse> login(Supplier<LoginRequest> request) {
        return handler.handle(200, () -> UserResponse.from(userService.login(request.get())));
    }
}
