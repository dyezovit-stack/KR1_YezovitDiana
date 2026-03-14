package com.orderprocessing.service;

import com.orderprocessing.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void notifyOrderConfirmed(Order order) {
        log.info("NOTIFICATION: Order {} confirmed for customer {}",
            order.getId(), order.getCustomerEmail());
    }

    @Override
    public void notifyPaymentReceived(Order order) {
        log.info("NOTIFICATION: Payment received for order {} — amount {}",
            order.getId(), order.getDiscountedAmount());
    }
}
