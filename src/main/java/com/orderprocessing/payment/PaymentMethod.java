package com.orderprocessing.payment;

import com.orderprocessing.domain.vo.Money;

public interface PaymentMethod {

    /**
     * Processes payment for the given amount.
     * @param amount the amount to charge
     * @return actual charged amount (may include commission)
     */
    Money processPayment(Money amount);

    /**
     * Returns the name of this payment method.
     */
    String getName();

    /**
     * Validates whether this payment method can handle the given amount.
     * @throws com.orderprocessing.exception.PaymentException if validation fails
     */
    void validate(Money amount);
}
