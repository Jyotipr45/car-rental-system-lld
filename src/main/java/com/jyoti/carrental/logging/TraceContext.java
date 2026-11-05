package com.jyoti.carrental.logging;

import java.util.UUID;

/**
 * Holds a short <b>trace id</b> for the request the current thread is serving.
 *
 * <p>Every log line written while handling one request carries the same id, so you can
 * pull the whole story of that request out of a busy log with one search:
 * {@code findstr "trace=3f9a1c2b" logs\car-rental.log}. This is the plain-Java twin of
 * SLF4J's MDC (Mapped Diagnostic Context) or Spring Cloud Sleuth / Micrometer tracing.</p>
 *
 * <p>It is a {@link ThreadLocal}: each thread sees its own id, so concurrent requests
 * never mix their ids. The caller that starts a trace must {@link #clear()} it in a
 * {@code finally} block, because pooled threads are reused.</p>
 *
 * @author Jyoti Prakash Parida
 */
public final class TraceContext {

    /** Shown when a log line is written outside any request (startup, tests). */
    public static final String NO_TRACE = "-";

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    /** Starts a new trace for this thread and returns its id, e.g. {@code 3f9a1c2b}. */
    public static String start() {
        return start("");
    }

    /** Starts a new trace with a readable prefix, e.g. {@code job-3f9a1c2b} for background jobs. */
    public static String start(String prefix) {
        String id = prefix + UUID.randomUUID().toString().substring(0, 8);
        TRACE_ID.set(id);
        return id;
    }

    /** @return the current thread's trace id, or {@value #NO_TRACE} when none is active */
    public static String current() {
        String id = TRACE_ID.get();
        return id == null ? NO_TRACE : id;
    }

    public static boolean isActive() {
        return TRACE_ID.get() != null;
    }

    /** Must be called when the request ends, so a reused thread doesn't inherit the old id. */
    public static void clear() {
        TRACE_ID.remove();
    }
}
