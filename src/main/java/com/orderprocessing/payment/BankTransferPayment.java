package com.orderprocessing.payment;

import com.orderprocessing.domain.vo.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class BankTransferPayment implements PaymentMethod {

    private static final Logger log = LoggerFactory.getLogger(BankTransferPayment.class);
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = BigDecimal.valueOf(50_000);
    private static final BigDecimal HIGH_COMMISSION_RATE = new BigDecimal("0.005"); // 0.5%
    private static final BigDecimal LOW_COMMISSION_RATE = new BigDecimal("0.02");   // 2%

    private final String bankAccount;

    public BankTransferPayment(String bankAccount) {
        if (bankAccount == null || bankAccount.isBlank()) {
            throw new IllegalArgumentException("Bank account must not be blank");
        }
        this.bankAccount = bankAccount;
    }

    @Override
    public void validate(Money amount) {
        // No specific restriction for bank transfer
    }

    @Override
    public Money processPayment(Money amount) {
        validate(amount);
        BigDecimal commissionRate = amount.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0
            ? HIGH_COMMISSION_RATE
            : LOW_COMMISSION_RATE;
        Money commission = amount.multiply(commissionRate);
        Money total = amount.add(commission);
        log.info("Processing BankTransfer ({}) for {} + commission {} = {}",
            bankAccount, amount, commission, total);
        return total;
    }

    @Override
    public String getName() {
        return "BankTransferPayment(" + bankAccount + ")";
    }

    public BigDecimal getCommissionRate(Money amount) {
        return amount.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0
            ? HIGH_COMMISSION_RATE
            : LOW_COMMISSION_RATE;
    }

    @Override
    public String toString() {
        return getName();
    }
}
