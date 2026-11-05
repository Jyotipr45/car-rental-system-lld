package com.jyoti.carrental.logging;

/**
 * Keeps personal data out of logs. Logs are read by many people and kept for a long time,
 * so they must never contain passwords, hashes, full emails or full licence numbers.
 *
 * @author Jyoti Prakash Parida
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    /** {@code riya.sharma@mail.com} → {@code r***@mail.com} */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return "-";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** {@code MH1220190001234} → {@code ***********1234} */
    public static String maskTail(String value) {
        if (value == null || value.isBlank()) return "-";
        int keep = Math.min(4, value.length());
        return "*".repeat(value.length() - keep) + value.substring(value.length() - keep);
    }
}
