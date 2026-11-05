package com.jyoti.carrental.model;

import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.VehicleCategory;

import java.math.BigDecimal;

/**
 * Concrete vehicle: a two-wheeler. Exists to show that the design is open for new
 * vehicle kinds without touching booking or pricing code.
 *
 * @author Jyoti Prakash Parida
 */
public class Bike extends Vehicle {

    private final int engineCc;

    public Bike(String registrationNo, String brand, String model, FuelType fuelType,
                BigDecimal pricePerDay, Long branchId, int engineCc) {
        super(registrationNo, brand, model, fuelType, pricePerDay, branchId);
        this.engineCc = engineCc;
    }

    @Override
    public VehicleCategory getCategory() {
        return VehicleCategory.BIKE;
    }

    @Override
    public int getSeatingCapacity() {
        return 2;
    }

    public int getEngineCc() { return engineCc; }
}
