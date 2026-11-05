package com.jyoti.carrental.repository;

import com.jyoti.carrental.model.Vehicle;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Vehicle} (cars, bikes, any future subclass).
 *
 * @author Jyoti Prakash Parida
 */
public class VehicleRepository extends InMemoryRepository<Vehicle> {

    public VehicleRepository(Clock clock) {
        super(clock);
    }

    public List<Vehicle> findByBranchId(Long branchId) {
        return findWhere(v -> v.getBranchId().equals(branchId));
    }

    public Optional<Vehicle> findByRegistrationNo(String registrationNo) {
        return findWhere(v -> v.getRegistrationNo().equalsIgnoreCase(registrationNo)).stream().findFirst();
    }
}
