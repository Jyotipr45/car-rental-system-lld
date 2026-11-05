package com.jyoti.carrental.model;

import com.jyoti.carrental.model.enums.CarType;
import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.TransmissionType;
import com.jyoti.carrental.model.enums.VehicleCategory;

import java.math.BigDecimal;

/**
 * Concrete vehicle: a car.
 *
 * @author Jyoti Prakash Parida
 */
public class Car extends Vehicle {

    private final CarType carType;
    private final TransmissionType transmission;
    private final int seats;

    public Car(String registrationNo, String brand, String model, FuelType fuelType,
               BigDecimal pricePerDay, Long branchId,
               CarType carType, TransmissionType transmission, int seats) {
        super(registrationNo, brand, model, fuelType, pricePerDay, branchId);
        this.carType = carType;
        this.transmission = transmission;
        this.seats = seats;
    }

    @Override
    public VehicleCategory getCategory() {
        return VehicleCategory.CAR;
    }

    @Override
    public int getSeatingCapacity() {
        return seats;
    }

    public CarType getCarType() { return carType; }
    public TransmissionType getTransmission() { return transmission; }
}
