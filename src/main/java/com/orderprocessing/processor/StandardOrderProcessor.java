package com.orderprocessing.processor;

import com.orderprocessing.domain.Order;
import com.orderprocessing.domain.OrderItem;
import com.orderprocessing.domain.vo.Money;
import com.orderprocessing.exception.ValidationException;
import com.orderprocessing.service.NotificationService;
import com.orderprocessing.service.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Standard order processor implementing rules from variant V06 (R1 + P6):
 *  - Max 10 items per order
 *  - 5% discount for orders >= 10_000 UAH
 */
public class StandardOrderProcessor extends OrderProcessorTemplate {

    private static final Logger log = LoggerFactory.getLogger(StandardOrderProcessor.class);

    private static final int MAX_ITEMS = 10;
    private static final Money DISCOUNT_THRESHOLD = new Money(10_000, "UAH");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05");

    // Composition — dependencies injected, not created with new inside methods
    private final StockRepository stockRepository;
    private final NotificationService notificationService;

    public StandardOrderProcessor(StockRepository stockRepository,
                                   NotificationService notificationService) {
        this.stockRepository = stockRepository;
        this.notificationService = notificationService;
    }

    @Override
    protected void validateOrder(Order order) {
        if (order.getItemCount() > MAX_ITEMS) {
            String msg = "Order " + order.getId() + " exceeds maximum item count: "
                + order.getItemCount() + " > " + MAX_ITEMS;
            log.warn("Validation failed: {}", msg);
            throw new ValidationException(msg);
        }
        if (order.getItemCount() == 0) {
            throw new ValidationException("Order must contain at least one item");
        }
        log.info("Order {} passed validation: {} item(s)", order.getId(), order.getItemCount());
    }

    @Override
    protected void reserveStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (!stockRepository.isAvailable(item)) {
                log.warn("Out of stock for product '{}' in order {}", item.getProductId(), order.getId());
            }
            // reserve() throws OutOfStockException with exception chaining if unavailable
            stockRepository.reserve(item);
        }
        log.info("All stock reserved for order {}", order.getId());
    }

    @Override
    protected void calculateTotal(Order order) {
        Money total = order.getTotalAmount();
        if (total.isGreaterThanOrEqual(DISCOUNT_THRESHOLD)) {
            order.applyDiscount(DISCOUNT_RATE);
            log.info("Applied 5% discount to order {}. Original: {}, After discount: {}",
                order.getId(), total, order.getDiscountedAmount());
        } else {
            log.info("No discount applied to order {}. Total: {}", order.getId(), total);
        }
    }

    @Override
    protected void complete(Order order) {
        notificationService.notifyPaymentReceived(order);
        notificationService.notifyOrderConfirmed(order);
        log.info("Order {} finalized and customer notified", order.getId());
    }
}
