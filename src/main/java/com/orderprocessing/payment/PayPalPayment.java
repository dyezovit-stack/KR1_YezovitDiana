package com.orderprocessing.payment;

import com.orderprocessing.domain.vo.Money;
import com.orderprocessing.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class PayPalPayment implements PaymentMethod {

    private static final Logger log = LoggerFactory.getLogger(PayPalPayment.class);
    private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(600);

    private final String paypalEmail;

    public PayPalPayment(String paypalEmail) {
        if (paypalEmail == null || paypalEmail.isBlank()) {
            throw new IllegalArgumentException("PayPal email must not be blank");
        }
        this.paypalEmail = paypalEmail;
    }

    @Override
    public void validate(Money amount) {
        if (amount.getAmount().compareTo(MIN_AMOUNT) < 0) {
            throw new PaymentException(
                "PayPalPayment: amount " + amount + " is below minimum " + MIN_AMOUNT + " UAH");
        }
    }

    @Override
    public Money processPayment(Money amount) {
        validate(amount);
        log.info("Processing PayPalPayment ({}) for {}", paypalEmail, amount);
        return amount;
    }

    @Override
    public String getName() {
        return "PayPalPayment(" + paypalEmail + ")";
    }

    @Override
    public String toString() {
        return getName();
    }
}
