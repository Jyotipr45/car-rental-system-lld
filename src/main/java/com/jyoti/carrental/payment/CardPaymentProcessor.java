package com.jyoti.carrental.payment;

import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.enums.PaymentMethod;

/**
 * Mock card gateway. A real one would call Razorpay/Stripe here.
 *
 * @author Jyoti Prakash Parida
 */
public class CardPaymentProcessor extends AbstractPaymentProcessor {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    protected PaymentResult doProcess(Payment payment) {
        return PaymentResult.success(newReference());
    }

    @Override
    protected PaymentResult doRefund(Payment payment) {
        return PaymentResult.success("RF-" + payment.getTransactionRef());
    }
}
