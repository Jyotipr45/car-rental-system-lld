package com.jyoti.carrental.dto.request;

import com.jyoti.carrental.util.Validate;

/**
 * Input for creating a branch (admin only).
 *
 * @author Jyoti Prakash Parida
 */
public record CreateBranchRequest(String name, String address, String city) {

    public CreateBranchRequest {
        name = Validate.notBlank(name, "name");
        address = Validate.notBlank(address, "address");
        city = Validate.notBlank(city, "city");
    }
}
