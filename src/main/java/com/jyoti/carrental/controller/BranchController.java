package com.jyoti.carrental.controller;

import com.jyoti.carrental.dto.request.CreateBranchRequest;
import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.dto.response.BranchResponse;
import com.jyoti.carrental.service.BranchService;

import java.util.List;
import java.util.function.Supplier;

/**
 * Branch endpoints.
 *
 * @author Jyoti Prakash Parida
 */
public class BranchController {

    private final BranchService branchService;
    private final GlobalExceptionHandler handler;

    public BranchController(BranchService branchService, GlobalExceptionHandler handler) {
        this.branchService = branchService;
        this.handler = handler;
    }

    /** POST /api/v1/branches (admin) */
    public ApiResponse<BranchResponse> create(Long adminId, Supplier<CreateBranchRequest> request) {
        return handler.handle(201, () -> BranchResponse.from(branchService.createBranch(adminId, request.get())));
    }

    /** GET /api/v1/branches */
    public ApiResponse<List<BranchResponse>> list() {
        return handler.handle(200, () -> branchService.getAllBranches().stream().map(BranchResponse::from).toList());
    }
}
