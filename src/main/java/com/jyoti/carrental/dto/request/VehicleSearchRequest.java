package com.jyoti.carrental.dto.request;

import com.jyoti.carrental.model.enums.CarType;
import com.jyoti.carrental.model.enums.VehicleCategory;
import com.jyoti.carrental.util.Validate;

import java.time.LocalDate;

/**
 * Search filters. {@code branchId} and dates are required; the rest are optional (null = any).
 *
 * @author Jyoti Prakash Parida
 */
public record VehicleSearchRequest(Long branchId, LocalDate startDate, LocalDate endDate,
                                   VehicleCategory category, CarType carType, Integer minSeats) {

    public VehicleSearchRequest {
        Validate.notNull(branchId, "branchId");
        Validate.notNull(startDate, "startDate");
        Validate.notNull(endDate, "endDate");
    }

    /** Convenience factory when only the required filters are needed. */
    public static VehicleSearchRequest of(Long branchId, LocalDate start, LocalDate end) {
        return new VehicleSearchRequest(branchId, start, end, null, null, null);
    }
}
