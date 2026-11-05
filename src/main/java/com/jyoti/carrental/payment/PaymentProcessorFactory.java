package com.jyoti.carrental.payment;

import com.jyoti.carrental.exception.InvalidRequestException;
import com.jyoti.carrental.model.enums.PaymentMethod;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Factory pattern.</b> Returns the right {@link PaymentProcessor} for a
 * {@link PaymentMethod} without any if-else or switch.
 *
 * <p>The map is built from whatever processors are passed in (in Spring you would inject
 * {@code List<PaymentProcessor>} and Spring finds every bean). Adding Net Banking later =
 * one new class registered in {@link com.jyoti.carrental.config.CarRentalSystem}; this
 * factory never changes.</p>
 *
 * @author Jyoti Prakash Parida
 */
public class PaymentProcessorFactory {

    private final Map<PaymentMethod, PaymentProcessor> processors;

    public PaymentProcessorFactory(List<PaymentProcessor> processorList) {
        Map<PaymentMethod, PaymentProcessor> map = new EnumMap<>(PaymentMethod.class);
        for (PaymentProcessor p : processorList) {
            map.put(p.supportedMethod(), p);
        }
        this.processors = Collections.unmodifiableMap(map);
    }

    public PaymentProcessor getProcessor(PaymentMethod method) {
        PaymentProcessor processor = processors.get(method);
        if (processor == null) {
            throw new InvalidRequestException("Unsupported payment method: " + method);
        }
        return processor;
    }
}
