package com.jyoti.carrental.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock tests can move forward. Because every service reads time from an injected
 * {@link Clock} (never {@code LocalDate.now()} directly), tests can say "15 minutes
 * pass" or "it is now the pickup day" without sleeping.
 *
 * @author Jyoti Prakash Parida
 */
public class MutableClock extends Clock {

    private Instant now;
    private final ZoneId zone;

    public MutableClock(Instant start, ZoneId zone) {
        this.now = start;
        this.zone = zone;
    }

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(now, zone);
    }

    @Override
    public synchronized Instant instant() {
        return now;
    }
}
