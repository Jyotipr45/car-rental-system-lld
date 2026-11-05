package com.jyoti.carrental.strategy.pricing;

import com.jyoti.carrental.model.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Weekend surcharge: Saturday and Sunday cost {@code pricePerDay × multiplier};
 * weekdays cost the normal rate.
 *
 * <p>Switching the whole system to this rule is a one-line change in
 * {@link com.jyoti.carrental.config.CarRentalSystem}. No service code changes.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class WeekendPricingStrategy implements PricingStrategy {

    private final BigDecimal weekendMultiplier;

    /** @param weekendMultiplier e.g. 1.25 for a 25% weekend surcharge */
    public WeekendPricingStrategy(BigDecimal weekendMultiplier) {
        this.weekendMultiplier = weekendMultiplier;
    }

    @Override
    public BigDecimal calculatePrice(Vehicle vehicle, LocalDate start, LocalDate end) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            BigDecimal rate = isWeekend(day)
                    ? vehicle.getPricePerDay().multiply(weekendMultiplier)
                    : vehicle.getPricePerDay();
            total = total.add(rate);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isWeekend(LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    @Override
    public String name() {
        return "WEEKEND_SURCHARGE";
    }
}
