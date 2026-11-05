package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.dto.request.AddBikeRequest;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.VehicleSearchRequest;
import com.jyoti.carrental.exception.DuplicateResourceException;
import com.jyoti.carrental.exception.ResourceNotFoundException;
import com.jyoti.carrental.model.Bike;
import com.jyoti.carrental.model.Car;
import com.jyoti.carrental.model.Vehicle;
import com.jyoti.carrental.model.enums.VehicleStatus;
import com.jyoti.carrental.repository.BookingRepository;
import com.jyoti.carrental.repository.VehicleRepository;
import com.jyoti.carrental.service.BranchService;
import com.jyoti.carrental.service.UserService;
import com.jyoti.carrental.service.VehicleService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Default {@link VehicleService}.
 *
 * @author Jyoti Prakash Parida
 */
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final BranchService branchService;
    private final UserService userService;
    private final Clock clock;

    public VehicleServiceImpl(VehicleRepository vehicleRepository, BookingRepository bookingRepository,
                              BranchService branchService, UserService userService, Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.branchService = branchService;
        this.userService = userService;
        this.clock = clock;
    }

    @Override
    public Vehicle addCar(Long adminId, AddCarRequest r) {
        userService.requireAdmin(adminId);
        branchService.getBranch(r.branchId()); // fails fast if the branch does not exist
        return saveUnique(new Car(r.registrationNo(), r.brand(), r.model(), r.fuelType(), r.pricePerDay(),
                r.branchId(), r.carType(), r.transmission(), r.seats()));
    }

    @Override
    public Vehicle addBike(Long adminId, AddBikeRequest r) {
        userService.requireAdmin(adminId);
        branchService.getBranch(r.branchId());
        return saveUnique(new Bike(r.registrationNo(), r.brand(), r.model(), r.fuelType(), r.pricePerDay(),
                r.branchId(), r.engineCc()));
    }

    /** Registration numbers are unique across the whole fleet. */
    private synchronized Vehicle saveUnique(Vehicle vehicle) {
        if (vehicleRepository.findByRegistrationNo(vehicle.getRegistrationNo()).isPresent()) {
            throw new DuplicateResourceException("Registration number already exists: " + vehicle.getRegistrationNo());
        }
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle changeStatus(Long adminId, Long vehicleId, VehicleStatus status) {
        userService.requireAdmin(adminId);
        Vehicle vehicle = getVehicle(vehicleId);
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle getVehicle(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }

    @Override
    public List<Vehicle> getVehiclesByBranch(Long branchId) {
        branchService.getBranch(branchId);
        return vehicleRepository.findByBranchId(branchId);
    }

    @Override
    public List<Vehicle> searchAvailable(VehicleSearchRequest q) {
        branchService.getBranch(q.branchId());
        BookingRules.validateDates(q.startDate(), q.endDate(), LocalDate.now(clock));

        return vehicleRepository.findByBranchId(q.branchId()).stream()
                .filter(Vehicle::isRentable)                                            // in service
                .filter(v -> q.category() == null || v.getCategory() == q.category())   // optional filters
                .filter(v -> q.carType() == null || (v instanceof Car c && c.getCarType() == q.carType()))
                .filter(v -> q.minSeats() == null || v.getSeatingCapacity() >= q.minSeats())
                .filter(v -> !bookingRepository.existsActiveOverlap(v.getId(), q.startDate(), q.endDate())) // free on dates
                .sorted(Comparator.comparing(Vehicle::getPricePerDay))
                .toList();
    }
}
