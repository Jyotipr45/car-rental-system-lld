package com.jyoti.carrental.strategy.pricing;

import com.jyoti.carrental.model.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Flat pricing: {@code pricePerDay × number of days}. Days are inclusive, so
 * 10 Nov → 12 Nov is 3 days.
 *
 * @author Jyoti Prakash Parida
 */
public class DailyPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(Vehicle vehicle, LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return vehicle.getPricePerDay()
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String name() {
        return "DAILY_FLAT";
    }
}
