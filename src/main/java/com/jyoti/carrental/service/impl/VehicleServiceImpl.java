package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.dto.request.AddBikeRequest;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.VehicleSearchRequest;
import com.jyoti.carrental.exception.DuplicateResourceException;
import com.jyoti.carrental.exception.ResourceNotFoundException;
import com.jyoti.carrental.logging.AuditAction;
import com.jyoti.carrental.logging.AuditLogger;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Default {@link VehicleService}.
 *
 * @author Jyoti Prakash Parida
 */
public class VehicleServiceImpl implements VehicleService {

    private static final Logger LOG = Logger.getLogger(VehicleServiceImpl.class.getName());

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final BranchService branchService;
    private final UserService userService;
    private final AuditLogger audit;
    private final Clock clock;

    public VehicleServiceImpl(VehicleRepository vehicleRepository, BookingRepository bookingRepository,
                              BranchService branchService, UserService userService, AuditLogger audit,
                              Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.branchService = branchService;
        this.userService = userService;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public Vehicle addCar(Long adminId, AddCarRequest r) {
        userService.requireAdmin(adminId);
        branchService.getBranch(r.branchId()); // fails fast if the branch does not exist
        return saveUnique(adminId, new Car(r.registrationNo(), r.brand(), r.model(), r.fuelType(), r.pricePerDay(),
                r.branchId(), r.carType(), r.transmission(), r.seats()));
    }

    @Override
    public Vehicle addBike(Long adminId, AddBikeRequest r) {
        userService.requireAdmin(adminId);
        branchService.getBranch(r.branchId());
        return saveUnique(adminId, new Bike(r.registrationNo(), r.brand(), r.model(), r.fuelType(), r.pricePerDay(),
                r.branchId(), r.engineCc()));
    }

    /** Registration numbers are unique across the whole fleet. */
    private synchronized Vehicle saveUnique(Long adminId, Vehicle vehicle) {
        if (vehicleRepository.findByRegistrationNo(vehicle.getRegistrationNo()).isPresent()) {
            audit.failure(AuditAction.VEHICLE_ADDED, adminId, "Vehicle", "duplicate registration " + vehicle.getRegistrationNo());
            throw new DuplicateResourceException("Registration number already exists: " + vehicle.getRegistrationNo());
        }
        Vehicle saved = vehicleRepository.save(vehicle);
        LOG.info(() -> saved.getCategory() + "#" + saved.getId() + " " + saved.getBrand() + " " + saved.getModel()
                + " added to Branch#" + saved.getBranchId() + " at Rs." + saved.getPricePerDay() + "/day");
        audit.success(AuditAction.VEHICLE_ADDED, adminId, "Vehicle#" + saved.getId(),
                saved.getRegistrationNo() + " " + saved.getBrand() + " " + saved.getModel() + " branch=" + saved.getBranchId());
        return saved;
    }

    @Override
    public Vehicle changeStatus(Long adminId, Long vehicleId, VehicleStatus status) {
        userService.requireAdmin(adminId);
        Vehicle vehicle = getVehicle(vehicleId);
        VehicleStatus old = vehicle.getStatus();
        vehicle.setStatus(status);
        vehicleRepository.save(vehicle);
        LOG.info(() -> "Vehicle#" + vehicleId + " status " + old + " -> " + status);
        audit.success(AuditAction.VEHICLE_STATUS_CHANGED, adminId, "Vehicle#" + vehicleId, old + " -> " + status);
        return vehicle;
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
        LOG.fine(() -> "Search branch=" + q.branchId() + " dates=" + q.startDate() + ".." + q.endDate()
                + " category=" + q.category() + " carType=" + q.carType() + " minSeats=" + q.minSeats());

        List<Vehicle> candidates = vehicleRepository.findByBranchId(q.branchId());
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : candidates) {
            String skipReason = rejectReason(v, q);
            if (skipReason == null) {
                result.add(v);
            } else {
                LOG.finest(() -> "  skip Vehicle#" + v.getId() + ": " + skipReason); // TRACE: why each one was dropped
            }
        }
        result.sort(Comparator.comparing(Vehicle::getPricePerDay));
        LOG.fine(() -> "Search checked " + candidates.size() + " vehicles, " + result.size() + " available");
        return result;
    }

    /** @return why the vehicle is excluded from search, or {@code null} if it matches */
    private String rejectReason(Vehicle v, VehicleSearchRequest q) {
        if (!v.isRentable()) return "status " + v.getStatus();
        if (q.category() != null && v.getCategory() != q.category()) return "category " + v.getCategory();
        if (q.carType() != null && !(v instanceof Car c && c.getCarType() == q.carType())) return "car type mismatch";
        if (q.minSeats() != null && v.getSeatingCapacity() < q.minSeats()) return "only " + v.getSeatingCapacity() + " seats";
        if (bookingRepository.existsActiveOverlap(v.getId(), q.startDate(), q.endDate())) return "already booked on these dates";
        return null;
    }
}
