package com.jyoti.carrental.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

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

    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Runs {@code action} while holding the lock for {@code vehicleId}.
     *
     * @return whatever the action returns
     */
    public <T> T withLock(Long vehicleId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(vehicleId, id -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock(); // always release, even when the action throws
        }
    }
}
