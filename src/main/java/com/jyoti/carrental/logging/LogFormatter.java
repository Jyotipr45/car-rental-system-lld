package com.jyoti.carrental.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * One log record per line, pipe-separated so it is easy to read and easy to grep:
 *
 * <pre>
 * 2026-11-05 15:40:01.123 | INFO  | main            | trace=3f9a1c2b | BookingServiceImpl   | Booking#4 created ...
 * 2026-11-05 15:40:01.125 | AUDIT | main            | trace=3f9a1c2b | audit                | action=BOOKING_CREATED ...
 * </pre>
 *
 * <p>Levels are printed with the familiar SLF4J names (TRACE, DEBUG, INFO, WARN, ERROR)
 * instead of JUL's FINEST/FINE/SEVERE, and audit records are tagged {@code AUDIT}.</p>
 *
 * <p>The trace id is read from {@link TraceContext} at format time. JUL handlers publish on
 * the calling thread, so this is the id of the request that produced the record.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class LogFormatter extends Formatter {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder(160);
        sb.append(TIME.format(Instant.ofEpochMilli(record.getMillis()))).append(" | ")
          .append(pad(levelName(record), 5)).append(" | ")
          .append(pad(Thread.currentThread().getName(), 15)).append(" | ")
          .append("trace=").append(pad(TraceContext.current(), 12)).append(" | ")
          .append(pad(shortName(record.getLoggerName()), 26)).append(" | ")
          .append(formatMessage(record))
          .append(System.lineSeparator());

        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }
        return sb.toString();
    }

    /** Maps JUL levels to the names every Java developer knows from SLF4J / Logback. */
    static String levelName(LogRecord record) {
        if (AuditLogger.LOGGER_NAME.equals(record.getLoggerName())) {
            return "AUDIT";
        }
        int v = record.getLevel().intValue();
        if (v >= Level.SEVERE.intValue()) return "ERROR";
        if (v >= Level.WARNING.intValue()) return "WARN";
        if (v >= Level.INFO.intValue()) return "INFO";
        if (v >= Level.FINE.intValue()) return "DEBUG";
        return "TRACE";
    }

    /** {@code com.jyoti.carrental.service.impl.BookingServiceImpl} → {@code BookingServiceImpl}. */
    private static String shortName(String loggerName) {
        if (loggerName == null) return "root";
        int dot = loggerName.lastIndexOf('.');
        return dot < 0 ? loggerName : loggerName.substring(dot + 1);
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}
