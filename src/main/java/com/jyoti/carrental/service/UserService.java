package com.jyoti.carrental.service;

import com.jyoti.carrental.dto.request.LoginRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.model.User;

/**
 * Registration, login and role checks.
 *
 * <p><b>Why do services have interfaces?</b> Controllers depend on {@code UserService},
 * not {@code UserServiceImpl}. The implementation can be swapped (or mocked in tests)
 * without touching callers. Dependency Inversion, the "D" in SOLID.</p>
 *
 * @author Jyoti Prakash Parida
 */
public interface UserService {

    User register(RegisterUserRequest request);

    User login(LoginRequest request);

    User getUser(Long userId);

    /** @throws com.jyoti.carrental.exception.AccessDeniedException if the user is not an admin */
    User requireAdmin(Long userId);
}
