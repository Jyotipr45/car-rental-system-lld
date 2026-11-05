package com.jyoti.carrental.payment;

import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.enums.PaymentMethod;

import java.math.BigDecimal;

/**
 * Mock UPI gateway. Declines anything above the per-transaction limit, which lets
 * the demo and tests exercise the "payment failed" path.
 *
 * @author Jyoti Prakash Parida
 */
public class UpiPaymentProcessor extends AbstractPaymentProcessor {

    /** Typical UPI per-transaction cap of ₹1,00,000. */
    public static final BigDecimal PER_TRANSACTION_LIMIT = new BigDecimal("100000");

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.UPI;
    }

    @Override
    protected PaymentResult doProcess(Payment payment) {
        if (payment.getAmount().compareTo(PER_TRANSACTION_LIMIT) > 0) {
            return PaymentResult.failure("UPI limit exceeded (max Rs." + PER_TRANSACTION_LIMIT + " per transaction)");
        }
        return PaymentResult.success(newReference());
    }

    @Override
    protected PaymentResult doRefund(Payment payment) {
        return PaymentResult.success("RF-" + payment.getTransactionRef());
    }
}
