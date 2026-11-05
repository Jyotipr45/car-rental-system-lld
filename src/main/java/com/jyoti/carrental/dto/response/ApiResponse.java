package com.jyoti.carrental.dto.response;

/**
 * Uniform envelope for every controller response, similar to what a REST API returns
 * as JSON: {@code {"status":201,"success":true,"message":"Created","data":{...}}}.
 *
 * @param status  HTTP-style status code
 * @param success true for 2xx
 * @param message short description or error text
 * @param data    payload (null on error)
 * @param <T>     payload type
 * @author Jyoti Prakash Parida
 */
public record ApiResponse<T>(int status, boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(int status, T data) {
        return new ApiResponse<>(status, true, status == 201 ? "Created" : "OK", data);
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, false, message, null);
    }

    @Override
    public String toString() {
        return success ? "HTTP " + status + " " + message + " -> " + data
                       : "HTTP " + status + " ERROR -> " + message;
    }
}
