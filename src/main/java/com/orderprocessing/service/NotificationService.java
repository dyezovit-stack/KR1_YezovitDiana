package com.orderprocessing.service;

import com.orderprocessing.domain.Order;

public interface NotificationService {
    void notifyOrderConfirmed(Order order);
    void notifyPaymentReceived(Order order);
}
