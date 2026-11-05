package com.jyoti.carrental.support;

import com.jyoti.carrental.logging.LogFormatter;
import com.jyoti.carrental.logging.LoggingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Collects every application log record (including audit) in memory, already formatted
 * exactly as it would appear in the log file, so tests can assert on log content.
 *
 * @author Jyoti Prakash Parida
 */
public final class LogCapture extends Handler {

    private static final Logger APP = Logger.getLogger(LoggingConfig.APP_LOGGER);
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private final LogFormatter formatter = new LogFormatter();

    /** Configures logging at TRACE with no console or files, and attaches a fresh capture. */
    public static LogCapture install() {
        LoggingConfig.init(Level.FINEST, Level.OFF, null);
        LogCapture capture = new LogCapture();
        capture.setLevel(Level.ALL);
        APP.addHandler(capture);
        return capture;
    }

    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record)) {
            lines.add(formatter.format(record).trim()); // formatted on the logging thread, so the trace id is correct
        }
    }

    public List<String> lines() {
        synchronized (lines) {
            return new ArrayList<>(lines);
        }
    }

    public List<String> matching(String text) {
        return lines().stream().filter(l -> l.contains(text)).toList();
    }

    public List<String> audit() {
        return matching("| AUDIT |");
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
