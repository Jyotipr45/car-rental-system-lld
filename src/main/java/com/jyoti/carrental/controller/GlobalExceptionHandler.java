package com.jyoti.carrental.controller;

import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.exception.CarRentalException;

import java.util.function.Supplier;

/**
 * One place that turns exceptions into error responses, the plain-Java twin of a
 * Spring {@code @RestControllerAdvice} with {@code @ExceptionHandler} methods.
 *
 * <p>Controllers wrap every call in {@link #handle}, so none of them contains try/catch.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class GlobalExceptionHandler {

    /**
     * @param successStatus status to return when the action succeeds (200 or 201)
     * @param action        the controller logic
     */
    public <T> ApiResponse<T> handle(int successStatus, Supplier<T> action) {
        try {
            return ApiResponse.ok(successStatus, action.get());
        } catch (CarRentalException e) {
            return ApiResponse.error(e.getHttpStatus(), e.getMessage()); // known business error
        } catch (RuntimeException e) {
            // Unknown bug: never leak internals to the client.
            System.err.println("Unexpected error: " + e);
            return ApiResponse.error(500, "Internal server error");
        }
    }
}
