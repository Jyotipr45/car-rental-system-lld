package com.jyoti.carrental.service.impl;

import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.enums.PaymentMethod;
import com.jyoti.carrental.model.enums.PaymentPurpose;
import com.jyoti.carrental.model.enums.PaymentStatus;
import com.jyoti.carrental.payment.PaymentProcessor;
import com.jyoti.carrental.payment.PaymentProcessorFactory;
import com.jyoti.carrental.payment.PaymentResult;
import com.jyoti.carrental.repository.PaymentRepository;
import com.jyoti.carrental.service.PaymentService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link PaymentService}. Picks the processor through the factory, so this
 * class has no idea whether it is talking to Card, UPI or Cash.
 *
 * @author Jyoti Prakash Parida
 */
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessorFactory processorFactory;
    private final Clock clock;

    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentProcessorFactory processorFactory,
                              Clock clock) {
        this.paymentRepository = paymentRepository;
        this.processorFactory = processorFactory;
        this.clock = clock;
    }

    @Override
    public Payment charge(Long bookingId, BigDecimal amount, PaymentMethod method, PaymentPurpose purpose) {
        Payment payment = new Payment(bookingId, amount, method, purpose);
        PaymentProcessor processor = processorFactory.getProcessor(method); // Factory → Strategy
        PaymentResult result = processor.process(payment);

        if (result.success()) {
            payment.markSuccess(result.transactionRef(), LocalDateTime.now(clock));
        } else {
            payment.markFailed(result.message());
        }
        return paymentRepository.save(payment); // failed attempts are kept for audit too
    }

    @Override
    public List<Payment> refundRentalPayments(Long bookingId) {
        List<Payment> refunded = new ArrayList<>();
        for (Payment p : paymentRepository.findByBookingId(bookingId)) {
            if (p.getPurpose() == PaymentPurpose.RENTAL && p.getStatus() == PaymentStatus.SUCCESS) {
                PaymentResult result = processorFactory.getProcessor(p.getMethod()).refund(p);
                if (result.success()) {
                    p.markRefunded();
                    paymentRepository.save(p);
                    refunded.add(p);
                }
            }
        }
        return refunded;
    }

    @Override
    public List<Payment> getPaymentsForBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }
}
