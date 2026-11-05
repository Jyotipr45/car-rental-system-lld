package com.jyoti.carrental.model;

import com.jyoti.carrental.exception.InvalidBookingStateException;
import com.jyoti.carrental.model.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The central "transaction" entity. It links <b>who</b> (user), <b>what</b> (vehicle),
 * <b>where</b> (branch), <b>when</b> (dates) and <b>money</b> (amounts, payments).
 *
 * <p>Most business systems have one such entity: Order in e-commerce, Reservation in
 * hotels, Appointment in clinics. Spotting it early is half of the design.</p>
 *
 * <p><b>Rich domain model:</b> status changes go through methods ({@link #confirm()},
 * {@link #startTrip}, ...) that enforce the state machine in {@link BookingStatus}.
 * There is no public {@code setStatus}, so invalid transitions are impossible.</p>
 *
 * <p>Date convention: {@code startDate} and {@code endDate} are both inclusive.
 * A booking for 10–12 Nov is 3 rental days and must be returned by the end of 12 Nov.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class Booking extends BaseEntity {

    private final Long userId;
    private final Long vehicleId;
    private final Long branchId;
    private final LocalDate startDate;
    private final LocalDate endDate;

    /** Null until the vehicle is handed over. */
    private LocalDateTime actualPickupTime;
    /** Null until the vehicle comes back. */
    private LocalDateTime actualReturnTime;

    private final BigDecimal baseAmount;
    private BigDecimal lateFee = BigDecimal.ZERO;
    private BigDecimal totalAmount;

    private BookingStatus status = BookingStatus.PENDING_PAYMENT;
    private String cancellationReason;

    public Booking(Long userId, Long vehicleId, Long branchId,
                   LocalDate startDate, LocalDate endDate, BigDecimal baseAmount) {
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.branchId = branchId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.baseAmount = baseAmount;
        this.totalAmount = baseAmount;
    }

    // ---------------------------------------------------------------- state transitions

    /** Payment succeeded: PENDING_PAYMENT → CONFIRMED. */
    public void confirm() {
        transitionTo(BookingStatus.CONFIRMED);
    }

    /** Vehicle handed over: CONFIRMED → ONGOING. */
    public void startTrip(LocalDateTime pickupTime) {
        transitionTo(BookingStatus.ONGOING);
        this.actualPickupTime = pickupTime;
    }

    /** Vehicle returned: ONGOING → COMPLETED. Adds any late fee to the total. */
    public void completeTrip(LocalDateTime returnTime, BigDecimal lateFee) {
        transitionTo(BookingStatus.COMPLETED);
        this.actualReturnTime = returnTime;
        this.lateFee = lateFee;
        this.totalAmount = baseAmount.add(lateFee);
    }

    /** Customer cancelled or payment timed out: → CANCELLED. */
    public void cancel(String reason) {
        transitionTo(BookingStatus.CANCELLED);
        this.cancellationReason = reason;
    }

    /** Single gatekeeper for every status change. */
    private void transitionTo(BookingStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidBookingStateException(
                    "Booking " + getId() + " cannot move from " + status + " to " + next);
        }
        this.status = next;
    }

    // ---------------------------------------------------------------- queries

    /**
     * Classic interval-overlap check, used by hotels, meeting rooms and clinics too:
     * two ranges overlap when {@code thisStart <= otherEnd && thisEnd >= otherStart}.
     */
    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !startDate.isAfter(otherEnd) && !endDate.isBefore(otherStart);
    }

    /** @return true if this booking currently holds the vehicle for its dates. */
    public boolean blocksInventory() {
        return status.blocksInventory();
    }

    public Long getUserId() { return userId; }
    public Long getVehicleId() { return vehicleId; }
    public Long getBranchId() { return branchId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDateTime getActualPickupTime() { return actualPickupTime; }
    public LocalDateTime getActualReturnTime() { return actualReturnTime; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public BigDecimal getLateFee() { return lateFee; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }

    @Override
    public String toString() {
        return "Booking{id=" + getId() + ", vehicle=" + vehicleId + ", " + startDate + "->" + endDate
                + ", total=Rs." + totalAmount + ", " + status + "}";
    }
}
