package com.jyoti.carrental.dto.request;

import com.jyoti.carrental.model.enums.CarType;
import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.TransmissionType;
import com.jyoti.carrental.util.Validate;

import java.math.BigDecimal;

/**
 * Input for adding a car to a branch (admin only).
 *
 * @author Jyoti Prakash Parida
 */
public record AddCarRequest(String registrationNo, String brand, String model, FuelType fuelType,
                            BigDecimal pricePerDay, Long branchId, CarType carType,
                            TransmissionType transmission, int seats) {

    public AddCarRequest {
        registrationNo = Validate.notBlank(registrationNo, "registrationNo").toUpperCase();
        brand = Validate.notBlank(brand, "brand");
        model = Validate.notBlank(model, "model");
        Validate.notNull(fuelType, "fuelType");
        Validate.positive(pricePerDay, "pricePerDay");
        Validate.notNull(branchId, "branchId");
        Validate.notNull(carType, "carType");
        Validate.notNull(transmission, "transmission");
        Validate.positive(seats, "seats");
    }
}
