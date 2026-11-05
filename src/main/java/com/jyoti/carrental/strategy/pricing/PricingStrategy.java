package com.jyoti.carrental.strategy.pricing;

import com.jyoti.carrental.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <b>Strategy pattern.</b> One job (price a rental), many ways to do it.
 *
 * <p>Why an interface? Pricing rules change often: weekend surcharge, festive season,
 * hourly rentals, coupons. Each rule becomes a new class implementing this interface,
 * and {@code BookingServiceImpl} never changes (Open/Closed principle). The alternative,
 * a growing if-else chain inside the service, is what this pattern avoids.</p>
 *
 * @author Jyoti Prakash Parida
 */
public interface PricingStrategy {

    /**
     * @param vehicle the vehicle being rented
     * @param start   first rental day (inclusive)
     * @param end     last rental day (inclusive)
     * @return the rental price, scale 2
     */
    BigDecimal calculatePrice(Vehicle vehicle, LocalDate start, LocalDate end);

    /** Human-readable name, for logs and docs. */
    String name();
}
