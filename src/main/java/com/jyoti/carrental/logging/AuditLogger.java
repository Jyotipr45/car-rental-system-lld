package com.jyoti.carrental.logging;

import java.util.logging.Logger;

/**
 * Writes one structured audit line per business action:
 *
 * <pre>
 * action=BOOKING_CREATED outcome=SUCCESS actor=user:2 target=Booking#4 details="vehicle=3 2026-11-10..2026-11-12 amount=8400.00"
 * </pre>
 *
 * <p>Key=value format so lines can be grepped today and parsed by ELK / Loki / Splunk later
 * without changes. The trace id is added by {@link LogFormatter}, so an audit line can be
 * matched with the debug lines of the same request.</p>
 *
 * <p><b>Rules:</b> never pass passwords, password hashes, card numbers or full emails here
 * (use {@link LogSanitizer}). Audit is written at INFO and is never switched off by the
 * app log level.</p>
 *
 * <p>Injected into services through their constructors (created once in
 * {@link com.jyoti.carrental.config.CarRentalSystem}), like any other dependency.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class AuditLogger {

    /** Dedicated logger name: its records go to {@code logs/audit.log} as well. */
    public static final String LOGGER_NAME = "com.jyoti.carrental.audit";

    private static final Logger AUDIT = Logger.getLogger(LOGGER_NAME);

    /**
     * @param action  what happened
     * @param outcome success / failure / denied
     * @param actorId user who did it ({@code null} = system or anonymous)
     * @param target  record affected, e.g. {@code Booking#4}
     * @param details short free text; no secrets
     */
    public void record(AuditAction action, AuditOutcome outcome, Long actorId, String target, String details) {
        String actor = actorId == null ? "system" : "user:" + actorId;
        AUDIT.info(() -> "action=" + action
                + " outcome=" + outcome
                + " actor=" + actor
                + " target=" + (target == null ? "-" : target)
                + " details=\"" + (details == null ? "" : details.replace("\"", "'")) + "\"");
    }

    public void success(AuditAction action, Long actorId, String target, String details) {
        record(action, AuditOutcome.SUCCESS, actorId, target, details);
    }

    public void failure(AuditAction action, Long actorId, String target, String details) {
        record(action, AuditOutcome.FAILURE, actorId, target, details);
    }

    public void denied(AuditAction action, Long actorId, String target, String details) {
        record(action, AuditOutcome.DENIED, actorId, target, details);
    }
}
