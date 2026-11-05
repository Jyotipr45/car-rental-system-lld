package com.jyoti.carrental.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted SHA-256 password hashing.
 *
 * <p>Good enough to show the rule "never store raw passwords". In production use
 * BCrypt or Argon2 (Spring Security's {@code BCryptPasswordEncoder}), which are slow on
 * purpose to resist brute force.</p>
 *
 * <p>Stored format: {@code base64(salt):base64(sha256(salt + password))}</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
        // utility class: no instances
    }

    public static String hash(String rawPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return encode(salt) + ":" + encode(digest(salt, rawPassword));
    }

    public static boolean matches(String rawPassword, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        // constant-time comparison avoids timing attacks
        return MessageDigest.isEqual(expected, digest(salt, rawPassword));
    }

    private static byte[] digest(byte[] salt, String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
