package com.jyoti.carrental.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * Sets up logging once at startup (the plain-Java twin of a {@code logback-spring.xml}).
 *
 * <p>Three outputs:</p>
 * <table>
 *   <tr><th>Output</th><th>What goes there</th><th>Level</th></tr>
 *   <tr><td>{@code logs/car-rental-0.log}</td><td>everything: app logs + audit</td><td>the app level (default INFO)</td></tr>
 *   <tr><td>{@code logs/audit-0.log}</td><td>only audit events (who did what, outcome)</td><td>always INFO</td></tr>
 *   <tr><td>console</td><td>whatever passes the console level</td><td>default ERROR, so the demo stays readable</td></tr>
 * </table>
 *
 * <p>Files roll over at 5 MB and keep 5 generations: {@code -0} is always the newest file,
 * older ones move to {@code -1 … -4}.</p>
 *
 * <p>Configure with system properties or environment variables:</p>
 * <pre>
 *   -Dlog.level=DEBUG     or  LOG_LEVEL=DEBUG     (TRACE, DEBUG, INFO, WARN, ERROR, OFF)
 *   -Dlog.console=DEBUG   or  LOG_CONSOLE=DEBUG
 *   -Dlog.dir=logs        or  LOG_DIR=logs
 * </pre>
 *
 * @author Jyoti Prakash Parida
 */
public final class LoggingConfig {

    /** Parent of every application logger, so one setting controls them all. */
    public static final String APP_LOGGER = "com.jyoti.carrental";

    // JUL holds loggers weakly; keep strong references or the configuration can be garbage-collected.
    private static final Logger APP = Logger.getLogger(APP_LOGGER);
    private static final Logger AUDIT = Logger.getLogger(AuditLogger.LOGGER_NAME);

    private LoggingConfig() {
    }

    /** Reads level, console level and directory from system properties / env vars, then initialises. */
    public static void initFromEnvironment() {
        initFromEnvironment("ERROR");
    }

    /** Same, with a different default console level (the API server shows INFO request lines). */
    public static void initFromEnvironment(String defaultConsoleLevel) {
        Level level = parseLevel(setting("log.level", "LOG_LEVEL", "INFO"));
        Level console = parseLevel(setting("log.console", "LOG_CONSOLE", defaultConsoleLevel));
        Path dir = Path.of(setting("log.dir", "LOG_DIR", "logs"));
        init(level, console, dir);
    }

    /**
     * @param appLevel     minimum level recorded at all (also the file level)
     * @param consoleLevel minimum level printed to the console ({@link Level#OFF} for none)
     * @param logDir       folder for log files, or {@code null} for no files (tests)
     */
    public static synchronized void init(Level appLevel, Level consoleLevel, Path logDir) {
        reset();
        APP.setUseParentHandlers(false); // don't also print through the JDK's default root handler
        APP.setLevel(appLevel);

        if (consoleLevel != Level.OFF) {
            Handler console = new StdoutHandler();
            console.setLevel(consoleLevel);
            APP.addHandler(console);
        }

        if (logDir != null) {
            try {
                Files.createDirectories(logDir);
                APP.addHandler(fileHandler(logDir.resolve("car-rental-%g.log"), appLevel));
                // Audit events also travel up to APP's handlers, so they appear in car-rental.log too.
                AUDIT.addHandler(fileHandler(logDir.resolve("audit-%g.log"), Level.INFO));
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot create log files in " + logDir.toAbsolutePath(), e);
            }
        }
        // Audit must never be filtered out by a quiet app level such as WARN.
        AUDIT.setLevel(Level.INFO);
    }

    /** Removes and closes every handler we added, so init can be called again (tests). */
    public static synchronized void reset() {
        for (Logger logger : new Logger[]{APP, AUDIT}) {
            for (Handler h : logger.getHandlers()) {
                logger.removeHandler(h);
                h.close();
            }
        }
    }

    /** {@code "DEBUG"} → {@link Level#FINE}. Accepts SLF4J names and JUL names. */
    public static Level parseLevel(String name) {
        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "TRACE", "FINEST", "ALL" -> Level.FINEST;
            case "DEBUG", "FINE" -> Level.FINE;
            case "INFO" -> Level.INFO;
            case "WARN", "WARNING" -> Level.WARNING;
            case "ERROR", "SEVERE" -> Level.SEVERE;
            case "OFF", "NONE" -> Level.OFF;
            default -> throw new IllegalArgumentException("Unknown log level: " + name);
        };
    }

    private static String setting(String property, String envVar, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVar);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static FileHandler fileHandler(Path file, Level level) throws IOException {
        FileHandler handler = new FileHandler(file.toString(), 5 * 1024 * 1024, 5, true);
        handler.setEncoding(StandardCharsets.UTF_8.name());
        handler.setFormatter(new LogFormatter());
        handler.setLevel(level);
        return handler;
    }

    /** Console handler that writes to stdout (not stderr) and flushes every line. */
    private static final class StdoutHandler extends StreamHandler {
        StdoutHandler() {
            super(new PrintStream(System.out, true, StandardCharsets.UTF_8), new LogFormatter());
        }

        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

        @Override
        public synchronized void close() {
            flush(); // never close System.out
        }
    }
}
