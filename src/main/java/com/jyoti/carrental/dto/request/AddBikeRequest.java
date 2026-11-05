package com.jyoti.carrental.dto.request;

import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.util.Validate;

import java.math.BigDecimal;

/**
 * Input for adding a bike to a branch (admin only).
 *
 * @author Jyoti Prakash Parida
 */
public record AddBikeRequest(String registrationNo, String brand, String model, FuelType fuelType,
                             BigDecimal pricePerDay, Long branchId, int engineCc) {

    public AddBikeRequest {
        registrationNo = Validate.notBlank(registrationNo, "registrationNo").toUpperCase();
        brand = Validate.notBlank(brand, "brand");
        model = Validate.notBlank(model, "model");
        Validate.notNull(fuelType, "fuelType");
        Validate.positive(pricePerDay, "pricePerDay");
        Validate.notNull(branchId, "branchId");
        Validate.positive(engineCc, "engineCc");
    }
}
