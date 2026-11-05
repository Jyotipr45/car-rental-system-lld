package com.jyoti.carrental.strategy.latefee;

import com.jyoti.carrental.model.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Each extra calendar day costs {@code pricePerDay × penaltyMultiplier}.
 * Returning any time on the due date is on time.
 *
 * @author Jyoti Prakash Parida
 */
public class StandardLateFeePolicy implements LateFeePolicy {

    private final BigDecimal penaltyMultiplier;

    /** @param penaltyMultiplier e.g. 1.5 means a late day costs 150% of the normal rate */
    public StandardLateFeePolicy(BigDecimal penaltyMultiplier) {
        this.penaltyMultiplier = penaltyMultiplier;
    }

    @Override
    public BigDecimal calculateLateFee(Vehicle vehicle, LocalDate dueDate, LocalDateTime actualReturnTime) {
        long lateDays = ChronoUnit.DAYS.between(dueDate, actualReturnTime.toLocalDate());
        if (lateDays <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return vehicle.getPricePerDay()
                .multiply(penaltyMultiplier)
                .multiply(BigDecimal.valueOf(lateDays))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
