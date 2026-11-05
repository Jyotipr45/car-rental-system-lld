package com.jyoti.carrental.scheduler;

import com.jyoti.carrental.service.BookingService;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background job that expires unpaid bookings so they stop blocking vehicles.
 * Plain-Java twin of a Spring {@code @Scheduled(fixedRate = 60000)} method.
 *
 * @author Jyoti Prakash Parida
 */
public class PendingBookingCleanupJob {

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
        return bookingService.expireUnpaidBookings(paymentWindow);
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
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** An exception would silently cancel a scheduled task, so catch and log instead. */
    private void safeRun() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            System.err.println("Cleanup job failed: " + e.getMessage());
        }
    }
}
