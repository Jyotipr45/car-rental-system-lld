package com.jyoti.carrental.model;

import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.VehicleCategory;
import com.jyoti.carrental.model.enums.VehicleStatus;

import java.math.BigDecimal;

/**
 * Anything that can be rented.
 *
 * <p><b>Why abstract?</b> Car and Bike both <i>are</i> vehicles and share most fields
 * (registration, brand, price, status, branch). Each also differs (a car has seats and a
 * body type; a bike has engine cc). Shared data + shared code + "never instantiate the
 * parent" = abstract class.</p>
 *
 * <p>Adding a new kind (Van, Truck) means adding a subclass, and the booking, pricing and
 * payment code keeps working unchanged because it depends only on {@code Vehicle}
 * (Open/Closed + Liskov substitution).</p>
 *
 * @author Jyoti Prakash Parida
 */
public abstract class Vehicle extends BaseEntity {

    /** Unique number plate, e.g. MH12AB1234. */
    private final String registrationNo;
    private final String brand;
    private final String model;
    private final FuelType fuelType;
    /** Money is always BigDecimal; double causes rounding errors. */
    private BigDecimal pricePerDay;
    private VehicleStatus status;
    /** Foreign key to {@link Branch}. */
    private final Long branchId;

    protected Vehicle(String registrationNo, String brand, String model, FuelType fuelType,
                      BigDecimal pricePerDay, Long branchId) {
        this.registrationNo = registrationNo;
        this.brand = brand;
        this.model = model;
        this.fuelType = fuelType;
        this.pricePerDay = pricePerDay;
        this.branchId = branchId;
        this.status = VehicleStatus.AVAILABLE;
    }

    /** @return which concrete kind this is (used by search filters). */
    public abstract VehicleCategory getCategory();

    /** @return how many people it carries, including the driver. */
    public abstract int getSeatingCapacity();

    /** @return true if the vehicle is in service (not under maintenance / inactive). */
    public boolean isRentable() {
        return status == VehicleStatus.AVAILABLE;
    }

    public String getRegistrationNo() { return registrationNo; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public FuelType getFuelType() { return fuelType; }
    public BigDecimal getPricePerDay() { return pricePerDay; }
    public VehicleStatus getStatus() { return status; }
    public Long getBranchId() { return branchId; }

    public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }
    public void setStatus(VehicleStatus status) { this.status = status; }

    @Override
    public String toString() {
        return getCategory() + "{id=" + getId() + ", " + brand + " " + model
                + ", reg=" + registrationNo + ", Rs." + pricePerDay + "/day, " + status + "}";
    }
}
