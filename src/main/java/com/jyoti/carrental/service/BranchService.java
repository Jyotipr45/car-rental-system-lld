package com.jyoti.carrental.service;

import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.model.Branch;

import java.util.List;

/**
 * Branch management.
 *
 * @author Jyoti Prakash Parida
 */
public interface BranchService {

    Branch createBranch(Long adminId, CreateBranchRequest request);

    Branch getBranch(Long branchId);

    List<Branch> getAllBranches();
}
