package com.orderprocessing.service;

import com.orderprocessing.domain.OrderItem;
import com.orderprocessing.exception.OutOfStockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class InMemoryStockRepository implements StockRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryStockRepository.class);

    private final Map<String, Integer> stock;

    public InMemoryStockRepository(Map<String, Integer> initialStock) {
        this.stock = new HashMap<>(initialStock);
    }

    public InMemoryStockRepository() {
        this.stock = new HashMap<>();
    }

    public void addStock(String productId, int quantity) {
        stock.merge(productId, quantity, Integer::sum);
    }

    @Override
    public boolean isAvailable(OrderItem item) {
        int available = stock.getOrDefault(item.getProductId(), 0);
        return available >= item.getQuantity();
    }

    @Override
    public void reserve(OrderItem item) {
        if (!isAvailable(item)) {
            throw new OutOfStockException(item.getProductId());
        }
        stock.merge(item.getProductId(), -item.getQuantity(), Integer::sum);
        log.info("Reserved {} x '{}' from stock", item.getQuantity(), item.getProductId());
    }

    public int getAvailable(String productId) {
        return stock.getOrDefault(productId, 0);
    }
}
