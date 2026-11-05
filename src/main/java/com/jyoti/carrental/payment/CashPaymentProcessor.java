package com.jyoti.carrental.payment;

import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.enums.PaymentMethod;

/**
 * Cash collected at the branch counter. Always succeeds; the reference acts as a receipt number.
 *
 * @author Jyoti Prakash Parida
 */
public class CashPaymentProcessor extends AbstractPaymentProcessor {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.CASH;
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
