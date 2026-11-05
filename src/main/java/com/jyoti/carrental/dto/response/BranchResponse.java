package com.jyoti.carrental.dto.response;

import com.jyoti.carrental.model.Branch;

/**
 * Public view of a branch.
 *
 * @author Jyoti Prakash Parida
 */
public record BranchResponse(Long id, String name, String address, String city) {

    public static BranchResponse from(Branch b) {
        return new BranchResponse(b.getId(), b.getName(), b.getAddress(), b.getCity());
    }
}
