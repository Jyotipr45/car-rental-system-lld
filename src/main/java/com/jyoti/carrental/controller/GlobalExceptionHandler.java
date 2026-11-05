package com.jyoti.carrental.controller;

import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.exception.CarRentalException;
import com.jyoti.carrental.logging.TraceContext;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point of every request. Does three jobs:
 *
 * <ol>
 *   <li><b>Tracing:</b> starts a trace id, logs the request start/end with its duration,
 *       and clears the id afterwards (like a servlet filter that fills the MDC).</li>
 *   <li><b>Error mapping:</b> turns exceptions into status codes, the plain-Java twin of a
 *       Spring {@code @RestControllerAdvice}.</li>
 *   <li><b>Error logging:</b> business errors (4xx) are logged at WARN with no stack trace,
 *       because they are expected; unexpected errors (500) are logged at ERROR with the full
 *       stack trace, and the client only gets the trace id, never internals.</li>
 * </ol>
 *
 * <p>Controllers wrap every call in {@link #handle}, so none of them contains try/catch.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class.getName());

    /**
     * @param operation     endpoint name for the logs, e.g. {@code "POST /api/v1/bookings"}
     * @param successStatus status to return when the action succeeds (200 or 201)
     * @param action        the controller logic
     */
    public <T> ApiResponse<T> handle(String operation, int successStatus, Supplier<T> action) {
        boolean ownsTrace = !TraceContext.isActive(); // nested calls reuse the caller's trace
        if (ownsTrace) {
            TraceContext.start();
        }
        long startNanos = System.nanoTime();
        LOG.fine(() -> "--> " + operation);
        try {
            ApiResponse<T> response = ApiResponse.ok(successStatus, action.get());
            LOG.info(() -> "<-- " + operation + " " + successStatus + " (" + elapsedMs(startNanos) + " ms)");
            return response;
        } catch (CarRentalException e) {
            LOG.warning(() -> "<-- " + operation + " " + e.getHttpStatus() + " " + e.getClass().getSimpleName()
                    + ": " + e.getMessage() + " (" + elapsedMs(startNanos) + " ms)");
            return ApiResponse.error(e.getHttpStatus(), e.getMessage());
        } catch (RuntimeException e) {
            // Unknown bug: full stack trace in the log, nothing internal in the response.
            LOG.log(Level.SEVERE, "<-- " + operation + " 500 unexpected error (" + elapsedMs(startNanos) + " ms)", e);
            return ApiResponse.error(500, "Internal server error");
        } finally {
            if (ownsTrace) {
                TraceContext.clear(); // threads are reused; never leak an old id
            }
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
