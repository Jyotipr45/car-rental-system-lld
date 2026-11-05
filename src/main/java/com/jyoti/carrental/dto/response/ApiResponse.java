package com.jyoti.carrental.dto.response;

import com.jyoti.carrental.logging.TraceContext;

/**
 * Uniform envelope for every controller response, similar to what a REST API returns
 * as JSON: {@code {"status":201,"success":true,"message":"Created","data":{...},"traceId":"3f9a1c2b"}}.
 *
 * <p>The {@code traceId} lets a customer or tester report a problem with one short code,
 * and support can find every log line of that request with a single search.</p>
 *
 * @param status  HTTP-style status code
 * @param success true for 2xx
 * @param message short description or error text
 * @param data    payload (null on error)
 * @param traceId id of the request in the logs
 * @param <T>     payload type
 * @author Jyoti Prakash Parida
 */
public record ApiResponse<T>(int status, boolean success, String message, T data, String traceId) {

    public static <T> ApiResponse<T> ok(int status, T data) {
        return new ApiResponse<>(status, true, status == 201 ? "Created" : "OK", data, TraceContext.current());
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, false, message, null, TraceContext.current());
    }

    @Override
    public String toString() {
        return success ? "HTTP " + status + " " + message + " -> " + data
                       : "HTTP " + status + " ERROR -> " + message + "  [traceId=" + traceId + "]";
    }
}
