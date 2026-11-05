package com.jyoti.carrental.controller;

import com.jyoti.carrental.dto.request.CreateBookingRequest;
import com.jyoti.carrental.dto.request.PaymentRequest;
import com.jyoti.carrental.dto.response.ApiResponse;
import com.jyoti.carrental.dto.response.BookingResponse;
import com.jyoti.carrental.dto.response.PaymentResponse;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.service.BookingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * Booking lifecycle endpoints.
 *
 * @author Jyoti Prakash Parida
 */
public class BookingController {

    private final BookingService bookingService;
    private final GlobalExceptionHandler handler;

    public BookingController(BookingService bookingService, GlobalExceptionHandler handler) {
        this.bookingService = bookingService;
        this.handler = handler;
    }

    /** POST /api/v1/bookings */
    public ApiResponse<BookingResponse> create(Supplier<CreateBookingRequest> request) {
        return handler.handle("POST /api/v1/bookings", 201, () -> BookingResponse.from(bookingService.createBooking(request.get())));
    }

    /** POST /api/v1/bookings/{id}/payments */
    public ApiResponse<PaymentResponse> pay(Supplier<PaymentRequest> request) {
        return handler.handle("POST /api/v1/bookings/{id}/payments", 201, () -> PaymentResponse.from(bookingService.payForBooking(request.get())));
    }

    /** POST /api/v1/bookings/{id}/cancel */
    public ApiResponse<BookingResponse> cancel(Long bookingId, Long userId) {
        return handler.handle("POST /api/v1/bookings/{id}/cancel", 200, () -> BookingResponse.from(bookingService.cancelBooking(bookingId, userId)));
    }

    /** POST /api/v1/bookings/{id}/pickup (admin) */
    public ApiResponse<BookingResponse> pickup(Long adminId, Long bookingId) {
        return handler.handle("POST /api/v1/bookings/{id}/pickup", 200, () -> BookingResponse.from(bookingService.markPickup(adminId, bookingId)));
    }

    /** POST /api/v1/bookings/{id}/return (admin) */
    public ApiResponse<BookingResponse> returnVehicle(Long adminId, Long bookingId, LocalDateTime returnTime,
                                                      PaymentMethod lateFeeMethod) {
        return handler.handle("POST /api/v1/bookings/{id}/return", 200, () -> BookingResponse.from(
                bookingService.markReturn(adminId, bookingId, returnTime, lateFeeMethod)));
    }

    /** GET /api/v1/bookings/{id} */
    public ApiResponse<BookingResponse> get(Long bookingId) {
        return handler.handle("GET /api/v1/bookings/{id}", 200, () -> BookingResponse.from(bookingService.getBooking(bookingId)));
    }

    /** GET /api/v1/users/{userId}/bookings */
    public ApiResponse<List<BookingResponse>> history(Long userId) {
        return handler.handle("GET /api/v1/users/{userId}/bookings", 200, () -> bookingService.getUserBookings(userId).stream()
                .map(BookingResponse::from).toList());
    }

    /** GET /api/v1/bookings/{id}/payments */
    public ApiResponse<List<PaymentResponse>> payments(Long bookingId) {
        return handler.handle("GET /api/v1/bookings/{id}/payments", 200, () -> bookingService.getPayments(bookingId).stream()
                .map(PaymentResponse::from).toList());
    }
}
