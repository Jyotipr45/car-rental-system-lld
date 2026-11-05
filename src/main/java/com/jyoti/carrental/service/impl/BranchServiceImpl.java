package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.exception.ResourceNotFoundException;
import com.jyoti.carrental.model.Branch;
import com.jyoti.carrental.repository.BranchRepository;
import com.jyoti.carrental.service.BranchService;
import com.jyoti.carrental.service.UserService;

import java.util.List;

/**
 * Default {@link BranchService}.
 *
 * @author Jyoti Prakash Parida
 */
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final UserService userService;

    public BranchServiceImpl(BranchRepository branchRepository, UserService userService) {
        this.branchRepository = branchRepository;
        this.userService = userService;
    }

    @Override
    public Branch createBranch(Long adminId, CreateBranchRequest request) {
        userService.requireAdmin(adminId);
        return branchRepository.save(new Branch(request.name(), request.address(), request.city()));
    }

    @Override
    public Branch getBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }

    @Override
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }
}
