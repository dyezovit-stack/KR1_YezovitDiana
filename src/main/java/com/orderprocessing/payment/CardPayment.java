package com.orderprocessing.payment;

import com.orderprocessing.domain.vo.Money;
import com.orderprocessing.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class CardPayment implements PaymentMethod {

    private static final Logger log = LoggerFactory.getLogger(CardPayment.class);
    private static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(45_000);

    private final String cardLastFour;

    public CardPayment(String cardLastFour) {
        if (cardLastFour == null || !cardLastFour.matches("\\d{4}")) {
            throw new IllegalArgumentException("Card last four digits must be exactly 4 digits");
        }
        this.cardLastFour = cardLastFour;
    }

    @Override
    public void validate(Money amount) {
        if (amount.getAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new PaymentException(
                "CardPayment: amount " + amount + " exceeds maximum allowed " + MAX_AMOUNT + " UAH");
        }
    }

    @Override
    public Money processPayment(Money amount) {
        validate(amount);
        log.info("Processing CardPayment (****{}) for {}", cardLastFour, amount);
        // No commission for card
        return amount;
    }

    @Override
    public String getName() {
        return "CardPayment(****" + cardLastFour + ")";
    }

    @Override
    public String toString() {
        return getName();
    }
}
