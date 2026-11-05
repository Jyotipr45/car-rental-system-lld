package com.jyoti.carrental.dto.response;

import com.jyoti.carrental.model.Car;
import com.jyoti.carrental.model.Vehicle;
import com.jyoti.carrental.model.enums.VehicleCategory;
import com.jyoti.carrental.model.enums.VehicleStatus;

import java.math.BigDecimal;

/**
 * Public view of a vehicle, flattened for the client.
 *
 * @author Jyoti Prakash Parida
 */
public record VehicleResponse(Long id, VehicleCategory category, String registrationNo, String brand,
                              String model, String type, int seats, BigDecimal pricePerDay,
                              VehicleStatus status, Long branchId) {

    public static VehicleResponse from(Vehicle v) {
        String type = (v instanceof Car car) ? car.getCarType().name() : v.getCategory().name();
        return new VehicleResponse(v.getId(), v.getCategory(), v.getRegistrationNo(), v.getBrand(),
                v.getModel(), type, v.getSeatingCapacity(), v.getPricePerDay(), v.getStatus(), v.getBranchId());
    }

    @Override
    public String toString() {
        return "#" + id + " " + brand + " " + model + " (" + type + ", " + seats + " seats) Rs." + pricePerDay + "/day";
    }
}
