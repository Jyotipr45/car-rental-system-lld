package com.jyoti.carrental.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Prevents double booking when two customers book the same vehicle at the same moment.
 *
 * <p><b>The race:</b> thread A checks "no overlap" → thread B checks "no overlap" →
 * both save → the car is booked twice. Checking and saving must be one atomic step.</p>
 *
 * <p><b>The fix:</b> one lock per vehicle. Only one thread at a time may run the
 * check-and-save for a given vehicle; bookings for <i>different</i> vehicles still run in
 * parallel. This is the in-memory twin of {@code SELECT ... FOR UPDATE} on the vehicle row
 * inside a {@code @Transactional} method (pessimistic locking) in a database-backed version.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class VehicleLockManager {

    private static final Logger LOG = Logger.getLogger(VehicleLockManager.class.getName());
    /** Waits longer than this are logged at DEBUG as contention. */
    private static final long CONTENTION_LOG_MS = 5;

    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Runs {@code action} while holding the lock for {@code vehicleId}.
     *
     * @return whatever the action returns
     */
    public <T> T withLock(Long vehicleId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(vehicleId, id -> new ReentrantLock());
        long waitStart = System.nanoTime();
        LOG.finest(() -> "Acquiring lock Vehicle#" + vehicleId + (lock.isLocked() ? " (busy, waiting)" : ""));
        lock.lock();
        long waitedMs = (System.nanoTime() - waitStart) / 1_000_000;
        if (waitedMs >= CONTENTION_LOG_MS) {
            LOG.fine(() -> "Lock Vehicle#" + vehicleId + " acquired after waiting " + waitedMs + " ms (contention)");
        } else {
            LOG.finest(() -> "Lock Vehicle#" + vehicleId + " acquired");
        }
        try {
            return action.get();
        } finally {
            lock.unlock(); // always release, even when the action throws
            LOG.finest(() -> "Lock Vehicle#" + vehicleId + " released");
        }
    }
}
