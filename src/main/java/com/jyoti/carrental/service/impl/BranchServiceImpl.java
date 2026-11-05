package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.exception.ResourceNotFoundException;
import com.jyoti.carrental.logging.AuditAction;
import com.jyoti.carrental.logging.AuditLogger;
import com.jyoti.carrental.model.Branch;
import com.jyoti.carrental.repository.BranchRepository;
import com.jyoti.carrental.service.BranchService;
import com.jyoti.carrental.service.UserService;

import java.util.List;
import java.util.logging.Logger;

/**
 * Default {@link BranchService}.
 *
 * @author Jyoti Prakash Parida
 */
public class BranchServiceImpl implements BranchService {

    private static final Logger LOG = Logger.getLogger(BranchServiceImpl.class.getName());

    private final BranchRepository branchRepository;
    private final UserService userService;
    private final AuditLogger audit;

    public BranchServiceImpl(BranchRepository branchRepository, UserService userService, AuditLogger audit) {
        this.branchRepository = branchRepository;
        this.userService = userService;
        this.audit = audit;
    }

    @Override
    public Branch createBranch(Long adminId, CreateBranchRequest request) {
        userService.requireAdmin(adminId);
        Branch branch = branchRepository.save(new Branch(request.name(), request.address(), request.city()));
        LOG.info(() -> "Branch#" + branch.getId() + " '" + branch.getName() + "' created in " + branch.getCity());
        audit.success(AuditAction.BRANCH_CREATED, adminId, "Branch#" + branch.getId(),
                branch.getName() + ", " + branch.getCity());
        return branch;
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
