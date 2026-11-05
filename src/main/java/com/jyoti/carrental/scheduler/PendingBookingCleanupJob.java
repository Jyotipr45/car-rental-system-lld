package com.jyoti.carrental.scheduler;

import com.jyoti.carrental.logging.TraceContext;
import com.jyoti.carrental.service.BookingService;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background job that expires unpaid bookings so they stop blocking vehicles.
 * Plain-Java twin of a Spring {@code @Scheduled(fixedRate = 60000)} method.
 *
 * @author Jyoti Prakash Parida
 */
public class PendingBookingCleanupJob {

    private static final Logger LOG = Logger.getLogger(PendingBookingCleanupJob.class.getName());

    private final BookingService bookingService;
    private final Duration paymentWindow;
    private ScheduledExecutorService executor;

    /** @param paymentWindow how long a customer has to pay, e.g. 15 minutes */
    public PendingBookingCleanupJob(BookingService bookingService, Duration paymentWindow) {
        this.bookingService = bookingService;
        this.paymentWindow = paymentWindow;
    }

    /** Runs one sweep now. Returns how many bookings were expired. */
    public int runOnce() {
        boolean ownsTrace = !TraceContext.isActive();
        if (ownsTrace) {
            TraceContext.start("job-"); // background work gets its own trace id too
        }
        try {
            int expired = bookingService.expireUnpaidBookings(paymentWindow);
            if (expired > 0) {
                LOG.info(() -> "Cleanup run expired " + expired + " unpaid booking(s)");
            } else {
                LOG.fine("Cleanup run: nothing to expire");
            }
            return expired;
        } finally {
            if (ownsTrace) {
                TraceContext.clear();
            }
        }
    }

    /** Starts running {@link #runOnce()} every {@code interval} on a daemon thread. */
    public synchronized void start(Duration interval) {
        if (executor != null) {
            return; // already running
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pending-booking-cleanup");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::safeRun, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        LOG.info(() -> "Cleanup job started: every " + interval.toSeconds() + " s, payment window "
                + paymentWindow.toMinutes() + " min");
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
            LOG.info("Cleanup job stopped");
        }
    }

    /** An exception would silently cancel a scheduled task, so catch and log instead. */
    private void safeRun() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Cleanup job run failed", e);
        }
    }
}
