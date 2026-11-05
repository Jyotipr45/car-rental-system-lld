package com.jyoti.carrental.util;

import com.jyoti.carrental.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Tiny input-validation helper, standing in for Bean Validation
 * ({@code @NotBlank}, {@code @NotNull}, {@code @Positive}, {@code @Email}) in Spring.
 * Request DTOs call these from their constructors so an invalid request can never exist.
 *
 * @author Jyoti Prakash Parida
 */
public final class Validate {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validate() {
    }

    public static String notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(field + " must not be blank");
        }
        return value.trim();
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new InvalidRequestException(field + " must not be null");
        }
        return value;
    }

    public static BigDecimal positive(BigDecimal value, String field) {
        notNull(value, field);
        if (value.signum() <= 0) {
            throw new InvalidRequestException(field + " must be greater than zero");
        }
        return value;
    }

    public static int positive(int value, String field) {
        if (value <= 0) {
            throw new InvalidRequestException(field + " must be greater than zero");
        }
        return value;
    }

    public static String email(String value) {
        notBlank(value, "email");
        if (!EMAIL.matcher(value.trim()).matches()) {
            throw new InvalidRequestException("email is not valid: " + value);
        }
        return value.trim().toLowerCase();
    }

    public static String minLength(String value, int min, String field) {
        notBlank(value, field);
        if (value.length() < min) {
            throw new InvalidRequestException(field + " must be at least " + min + " characters");
        }
        return value;
    }
}
