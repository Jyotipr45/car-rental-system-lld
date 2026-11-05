package com.jyoti.carrental.payment;

import com.jyoti.carrental.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * <b>Template Method pattern.</b> Every payment follows the same skeleton:
 * validate → call gateway → log. Only the gateway call differs per method.
 *
 * <p>{@link #process} and {@link #refund} are {@code final} so subclasses cannot change
 * the skeleton (e.g. skip validation); they only fill in {@link #doProcess} and
 * {@link #doRefund}.</p>
 *
 * <p>This class also shows the common pairing: the <i>interface</i> defines the
 * contract, the <i>abstract class</i> provides the shared code.</p>
 *
 * @author Jyoti Prakash Parida
 */
public abstract class AbstractPaymentProcessor implements PaymentProcessor {

    private static final Logger LOG = Logger.getLogger(AbstractPaymentProcessor.class.getName());

    @Override
    public final PaymentResult process(Payment payment) {
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentResult.failure("Amount must be greater than zero");
        }
        PaymentResult result = doProcess(payment);
        LOG.fine(() -> supportedMethod() + " payment for booking " + payment.getBookingId()
                + " -> " + (result.success() ? "SUCCESS " + result.transactionRef() : "FAILED " + result.message()));
        return result;
    }

    @Override
    public final PaymentResult refund(Payment payment) {
        if (payment.getTransactionRef() == null) {
            return PaymentResult.failure("Nothing to refund: payment never succeeded");
        }
        PaymentResult result = doRefund(payment);
        LOG.fine(() -> supportedMethod() + " refund for " + payment.getTransactionRef() + " -> " + result.success());
        return result;
    }

    /** The step that varies: talk to the actual gateway. */
    protected abstract PaymentResult doProcess(Payment payment);

    /** The step that varies: reverse a successful payment. */
    protected abstract PaymentResult doRefund(Payment payment);

    /** Shared helper: builds a gateway-style reference such as {@code UPI-3F9A1C2B}. */
    protected String newReference() {
        return supportedMethod() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
