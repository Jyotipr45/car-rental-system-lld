package com.jyoti.carrental.repository;

import com.jyoti.carrental.model.User;

import java.time.Clock;
import java.util.Optional;

/**
 * Data access for {@link User}.
 *
 * @author Jyoti Prakash Parida
 */
public class UserRepository extends InMemoryRepository<User> {

    public UserRepository(Clock clock) {
        super(clock);
    }

    /** Emails are compared case-insensitively. */
    public Optional<User> findByEmail(String email) {
        return findWhere(u -> u.getEmail().equalsIgnoreCase(email)).stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
}
