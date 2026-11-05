package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.dto.request.LoginRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.exception.AccessDeniedException;
import com.jyoti.carrental.exception.AuthenticationException;
import com.jyoti.carrental.exception.DuplicateResourceException;
import com.jyoti.carrental.exception.ResourceNotFoundException;
import com.jyoti.carrental.model.User;
import com.jyoti.carrental.repository.UserRepository;
import com.jyoti.carrental.service.UserService;
import com.jyoti.carrental.util.PasswordHasher;

/**
 * Default {@link UserService}.
 *
 * <p>Dependencies arrive through the constructor (constructor injection), never via
 * {@code new} inside the class. That keeps it testable and is exactly how Spring wires
 * {@code @Service} beans.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public synchronized User register(RegisterUserRequest request) {
        // synchronized: check-then-insert must be atomic so two sign-ups can't take one email
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        User user = new User(request.name(), request.email(), request.phone(),
                PasswordHasher.hash(request.password()), request.drivingLicenseNo(), request.role());
        return userRepository.save(user);
    }

    @Override
    public User login(LoginRequest request) {
        // Same message for "no such email" and "wrong password" so attackers can't probe which emails exist.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        if (!PasswordHasher.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }
        return user;
    }

    @Override
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Override
    public User requireAdmin(Long userId) {
        User user = getUser(userId);
        if (!user.isAdmin()) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
        return user;
    }
}
