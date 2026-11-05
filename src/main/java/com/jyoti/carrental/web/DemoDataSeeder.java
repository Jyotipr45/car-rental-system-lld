package com.jyoti.carrental.web;

import com.jyoti.carrental.config.CarRentalSystem;
import com.jyoti.carrental.dto.request.AddBikeRequest;
import com.jyoti.carrental.dto.request.AddCarRequest;
import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.dto.request.RegisterUserRequest;
import com.jyoti.carrental.model.enums.CarType;
import com.jyoti.carrental.model.enums.FuelType;
import com.jyoti.carrental.model.enums.TransmissionType;
import com.jyoti.carrental.model.enums.UserRole;

import java.math.BigDecimal;

/**
 * Loads a known starting state so manual testing can begin immediately.
 * The ids are predictable because the store starts empty: users 1–3, branch 1, vehicles 1–5.
 *
 * @author Jyoti Prakash Parida
 */
public final class DemoDataSeeder {

    /** Ids of the seeded records, printed at startup and used by the Postman collection. */
    public record SeedData(Long adminId, Long riyaId, Long arjunId, Long branchId,
                           Long swiftId, Long cretaId, Long nexonId, Long bikeId, Long bmwId) {
    }

    private DemoDataSeeder() {
    }

    public static SeedData seed(CarRentalSystem system) {
        var users = system.userService();
        Long admin = users.register(new RegisterUserRequest("Admin Pune", "admin@rental.com", "9000000001",
                "admin@123", null, UserRole.ADMIN)).getId();
        Long riya = users.register(new RegisterUserRequest("Riya Sharma", "riya@mail.com", "9000000002",
                "riya@123", "MH1220190001234", UserRole.CUSTOMER)).getId();
        Long arjun = users.register(new RegisterUserRequest("Arjun Mehta", "arjun@mail.com", "9000000003",
                "arjun@123", "MH1220180005678", UserRole.CUSTOMER)).getId();

        Long branch = system.branchService().createBranch(admin,
                new CreateBranchRequest("Hinjewadi Hub", "Phase 1, Hinjewadi", "Pune")).getId();

        var vehicles = system.vehicleService();
        Long swift = vehicles.addCar(admin, new AddCarRequest("MH12AB1001", "Maruti", "Swift", FuelType.PETROL,
                new BigDecimal("1800"), branch, CarType.HATCHBACK, TransmissionType.MANUAL, 5)).getId();
        Long creta = vehicles.addCar(admin, new AddCarRequest("MH12AB1002", "Hyundai", "Creta", FuelType.DIESEL,
                new BigDecimal("3200"), branch, CarType.SUV, TransmissionType.AUTOMATIC, 5)).getId();
        Long nexon = vehicles.addCar(admin, new AddCarRequest("MH12AB1003", "Tata", "Nexon EV", FuelType.ELECTRIC,
                new BigDecimal("2800"), branch, CarType.SUV, TransmissionType.AUTOMATIC, 5)).getId();
        Long bike = vehicles.addBike(admin, new AddBikeRequest("MH12XY9001", "Royal Enfield", "Classic 350",
                FuelType.PETROL, new BigDecimal("900"), branch, 350)).getId();
        Long bmw = vehicles.addCar(admin, new AddCarRequest("MH12LX0007", "BMW", "7 Series", FuelType.PETROL,
                new BigDecimal("40000"), branch, CarType.LUXURY, TransmissionType.AUTOMATIC, 5)).getId();

        return new SeedData(admin, riya, arjun, branch, swift, creta, nexon, bike, bmw);
    }
}
