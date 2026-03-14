package com.orderprocessing.processor;

import com.orderprocessing.domain.Order;
import com.orderprocessing.payment.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template Method pattern.
 * Defines the fixed skeleton of order processing.
 * Subclasses implement abstract steps; some steps have default implementations.
 */
public abstract class OrderProcessorTemplate {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessorTemplate.class);

    /**
     * Final template method — defines the immutable processing pipeline.
     */
    public final void process(Order order, PaymentMethod paymentMethod) {
        log.info("=== Starting order processing for order {} ===", order.getId());

        validateOrder(order);
        log.info("[STEP 1] Validation passed for order {}", order.getId());

        reserveStock(order);
        log.info("[STEP 2] Stock reserved for order {}", order.getId());

        calculateTotal(order);
        log.info("[STEP 3] Total calculated: {}", order.getDiscountedAmount());

        processPayment(order, paymentMethod);
        log.info("[STEP 4] Payment processed for order {}", order.getId());

        complete(order);
        log.info("=== Order {} processing complete. Status: {} ===", order.getId(), order.getStatus());
    }

    /**
     * Abstract — subclass must define validation rules.
     */
    protected abstract void validateOrder(Order order);

    /**
     * Abstract — subclass must define stock reservation logic (required by R1 variant).
     */
    protected abstract void reserveStock(Order order);

    /**
     * Abstract — subclass must define discount/calculation logic.
     */
    protected abstract void calculateTotal(Order order);

    /**
     * Default payment step — delegates to PaymentMethod, then marks order as PAID.
     * Can be overridden if needed.
     */
    protected void processPayment(Order order, PaymentMethod paymentMethod) {
        paymentMethod.processPayment(order.getDiscountedAmount());
        order.markAsPaid();
    }

    /**
     * Default completion step — sends notification.
     * Can be overridden by subclasses.
     */
    protected abstract void complete(Order order);
}
