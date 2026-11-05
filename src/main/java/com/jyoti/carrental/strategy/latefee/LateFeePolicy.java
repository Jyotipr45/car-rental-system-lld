package com.jyoti.carrental.strategy.latefee;

import com.jyoti.carrental.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <b>Strategy pattern</b> for penalties on late return. Kept separate from
 * {@link com.jyoti.carrental.strategy.pricing.PricingStrategy} because the two rules
 * change independently (Single Responsibility).
 *
 * @author Jyoti Prakash Parida
 */
public interface LateFeePolicy {

    /**
     * @param vehicle          the rented vehicle
     * @param dueDate          last booked day; the vehicle is due back by the end of this day
     * @param actualReturnTime when it actually came back
     * @return late fee, or {@code BigDecimal.ZERO} when on time
     */
    BigDecimal calculateLateFee(Vehicle vehicle, LocalDate dueDate, LocalDateTime actualReturnTime);
}
