package com.jyoti.carrental.controller;

import com.jyoti.carrental.dto.request.AddBikeRequest;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.VehicleSearchRequest;
import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.dto.response.VehicleResponse;
import com.jyoti.carrental.model.enums.VehicleStatus;
import com.jyoti.carrental.service.VehicleService;

import java.util.List;
import java.util.function.Supplier;

/**
 * Fleet and search endpoints.
 *
 * @author Jyoti Prakash Parida
 */
public class VehicleController {

    private final VehicleService vehicleService;
    private final GlobalExceptionHandler handler;

    public VehicleController(VehicleService vehicleService, GlobalExceptionHandler handler) {
        this.vehicleService = vehicleService;
        this.handler = handler;
    }

    /** POST /api/v1/vehicles/cars (admin) */
    public ApiResponse<VehicleResponse> addCar(Long adminId, Supplier<AddCarRequest> request) {
        return handler.handle("POST /api/v1/vehicles/cars", 201, () -> VehicleResponse.from(vehicleService.addCar(adminId, request.get())));
    }

    /** POST /api/v1/vehicles/bikes (admin) */
    public ApiResponse<VehicleResponse> addBike(Long adminId, Supplier<AddBikeRequest> request) {
        return handler.handle("POST /api/v1/vehicles/bikes", 201, () -> VehicleResponse.from(vehicleService.addBike(adminId, request.get())));
    }

    /** PATCH /api/v1/vehicles/{id}/status (admin) */
    public ApiResponse<VehicleResponse> changeStatus(Long adminId, Long vehicleId, VehicleStatus status) {
        return handler.handle("PATCH /api/v1/vehicles/{id}/status", 200, () -> VehicleResponse.from(vehicleService.changeStatus(adminId, vehicleId, status)));
    }

    /** GET /api/v1/branches/{id}/vehicles */
    public ApiResponse<List<VehicleResponse>> listByBranch(Long branchId) {
        return handler.handle("GET /api/v1/branches/{id}/vehicles", 200, () -> vehicleService.getVehiclesByBranch(branchId)
                .stream().map(VehicleResponse::from).toList());
    }

    /** GET /api/v1/vehicles/search?branchId=&amp;start=&amp;end=&amp;category=&amp;carType=&amp;minSeats= */
    public ApiResponse<List<VehicleResponse>> search(Supplier<VehicleSearchRequest> request) {
        return handler.handle("GET /api/v1/vehicles/search", 200, () -> vehicleService.searchAvailable(request.get()).stream()
                .map(VehicleResponse::from).toList());
    }
}
