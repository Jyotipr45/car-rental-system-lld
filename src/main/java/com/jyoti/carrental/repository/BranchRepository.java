package com.jyoti.carrental.repository;

import com.jyoti.carrental.model.Branch;

import java.time.Clock;
import java.util.List;

/**
 * Data access for {@link Branch}.
 *
 * @author Jyoti Prakash Parida
 */
public class BranchRepository extends InMemoryRepository<Branch> {

    public BranchRepository(Clock clock) {
        super(clock);
    }

    public List<Branch> findByCity(String city) {
        return findWhere(b -> b.getCity().equalsIgnoreCase(city));
    }
}
