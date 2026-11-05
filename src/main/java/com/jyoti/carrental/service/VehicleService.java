package com.jyoti.carrental.service;

import com.jyoti.carrental.dto.request.AddBikeRequest;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.VehicleSearchRequest;
import com.jyoti.carrental.model.Vehicle;
import com.jyoti.carrental.model.enums.VehicleStatus;

import java.util.List;

/**
 * Fleet management and availability search.
 *
 * @author Jyoti Prakash Parida
 */
public interface VehicleService {

    Vehicle addCar(Long adminId, AddCarRequest request);

    Vehicle addBike(Long adminId, AddBikeRequest request);

    Vehicle changeStatus(Long adminId, Long vehicleId, VehicleStatus status);

    Vehicle getVehicle(Long vehicleId);

    List<Vehicle> getVehiclesByBranch(Long branchId);

    /**
     * Vehicles at the branch that are in service, match the filters and have no active
     * booking overlapping the dates. Sorted by price, cheapest first.
     */
    List<Vehicle> searchAvailable(VehicleSearchRequest request);
}
