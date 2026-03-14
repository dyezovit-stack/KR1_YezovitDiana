package com.orderprocessing.exception;

public class OrderNotFoundException extends AppException {
    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
