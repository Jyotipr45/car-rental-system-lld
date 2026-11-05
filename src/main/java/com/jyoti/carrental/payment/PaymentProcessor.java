package com.jyoti.carrental.payment;

import com.jyoti.carrental.model.Payment;
import com.jyoti.carrental.model.enums.PaymentMethod;

/**
 * Contract for a payment gateway integration. <b>Strategy pattern</b>: Card, UPI and
 * Cash each "process a payment" differently, and the caller does not care which one
 * it talks to.
 *
 * @author Jyoti Prakash Parida
 */
public interface PaymentProcessor {

    /** The method this processor handles. Used by the factory to build its lookup map. */
    PaymentMethod supportedMethod();

    PaymentResult process(Payment payment);

    PaymentResult refund(Payment payment);
}
