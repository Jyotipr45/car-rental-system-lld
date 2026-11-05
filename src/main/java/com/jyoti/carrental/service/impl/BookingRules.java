package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.exception.InvalidRequestException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Date rules shared by search and booking, kept in one place so they can never disagree.
 *
 * @author Jyoti Prakash Parida
 */
final class BookingRules {

    /** Longest single rental. */
    static final int MAX_RENTAL_DAYS = 30;
    /** How far ahead a customer may book. */
    static final int MAX_ADVANCE_DAYS = 90;

    private BookingRules() {
    }

    static void validateDates(LocalDate start, LocalDate end, LocalDate today) {
        if (start.isBefore(today)) {
            throw new InvalidRequestException("Start date cannot be in the past");
        }
        if (end.isBefore(start)) {
            throw new InvalidRequestException("End date cannot be before start date");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RENTAL_DAYS) {
            throw new InvalidRequestException("A rental cannot be longer than " + MAX_RENTAL_DAYS + " days");
        }
        if (start.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new InvalidRequestException("Bookings open only " + MAX_ADVANCE_DAYS + " days in advance");
        }
    }
}
