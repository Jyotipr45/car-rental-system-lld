package com.jyoti.carrental.model.enums;

/**
 * Why money moved. One booking can have a RENTAL payment and, if returned late,
 * a LATE_FEE payment. That is why Booking → Payment is 1-to-many.
 *
 * @author Jyoti Prakash Parida
 */
public enum PaymentPurpose {
    RENTAL,
    LATE_FEE
}
