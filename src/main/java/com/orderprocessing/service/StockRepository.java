package com.orderprocessing.service;

import com.orderprocessing.domain.OrderItem;

public interface StockRepository {
    /**
     * Checks if the product is available in the required quantity.
     * @param item the order item to check
     * @return true if stock is sufficient
     */
    boolean isAvailable(OrderItem item);

    /**
     * Reserves stock for the item.
     * @param item the order item to reserve
     * @throws com.orderprocessing.exception.OutOfStockException if not enough stock
     */
    void reserve(OrderItem item);
}
