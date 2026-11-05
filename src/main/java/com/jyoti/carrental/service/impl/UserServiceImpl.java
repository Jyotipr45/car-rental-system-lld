package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.dto.request.LoginRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.exception.AccessDeniedException;
import com.jyoti.carrental.exception.AuthenticationException;
import com.jyoti.carrental.exception.DuplicateResourceException;
import com.jyoti.carrental.exception.ResourceNotFoundException;
import com.jyoti.carrental.logging.AuditAction;
import com.jyoti.carrental.logging.AuditLogger;
import com.jyoti.carrental.model.User;
import com.jyoti.carrental.repository.UserRepository;
import com.jyoti.carrental.service.UserService;
import com.jyoti.carrental.util.PasswordHasher;

import java.util.Optional;
import java.util.logging.Logger;

import static com.jyoti.carrental.logging.LogSanitizer.maskEmail;

/**
 * Default {@link UserService}.
 *
 * <p>Dependencies arrive through the constructor (constructor injection), never via
 * {@code new} inside the class. That keeps it testable and is exactly how Spring wires
 * {@code @Service} beans.</p>
 *
 * <p><b>Logging rule here:</b> emails are always masked and passwords/hashes are never logged.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class UserServiceImpl implements UserService {

    private static final Logger LOG = Logger.getLogger(UserServiceImpl.class.getName());

    private final UserRepository userRepository;
    private final AuditLogger audit;

    public UserServiceImpl(UserRepository userRepository, AuditLogger audit) {
        this.userRepository = userRepository;
        this.audit = audit;
    }

    @Override
    public synchronized User register(RegisterUserRequest request) {
        // synchronized: check-then-insert must be atomic so two sign-ups can't take one email
        LOG.fine(() -> "Registering " + request.role() + " email=" + maskEmail(request.email()));
        if (userRepository.existsByEmail(request.email())) {
            audit.failure(AuditAction.USER_REGISTERED, null, "User", "duplicate email " + maskEmail(request.email()));
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        User user = userRepository.save(new User(request.name(), request.email(), request.phone(),
                PasswordHasher.hash(request.password()), request.drivingLicenseNo(), request.role()));
        LOG.info(() -> "User#" + user.getId() + " registered as " + user.getRole());
        audit.success(AuditAction.USER_REGISTERED, user.getId(), "User#" + user.getId(), "role=" + user.getRole());
        return user;
    }

    @Override
    public User login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(request.email());
        if (found.isEmpty()) {
            // The audit keeps the real reason; the client gets one generic message
            // so attackers can't probe which emails exist.
            audit.failure(AuditAction.LOGIN, null, "User", "unknown email " + maskEmail(request.email()));
            throw new AuthenticationException("Invalid email or password");
        }
        User user = found.get();
        if (!PasswordHasher.matches(request.password(), user.getPasswordHash())) {
            audit.failure(AuditAction.LOGIN, user.getId(), "User#" + user.getId(), "wrong password");
            throw new AuthenticationException("Invalid email or password");
        }
        audit.success(AuditAction.LOGIN, user.getId(), "User#" + user.getId(), "role=" + user.getRole());
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
            audit.denied(AuditAction.ACCESS_DENIED, userId, "admin-action", "role=" + user.getRole());
            throw new AccessDeniedException("Only admins can perform this action");
        }
        LOG.finest(() -> "Admin check passed for User#" + userId);
        return user;
    }
}
