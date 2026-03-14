package com.orderprocessing.service;

import com.orderprocessing.domain.Order;
import com.orderprocessing.exception.InfrastructureException;

import java.util.*;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new HashMap<>();

    @Override
    public void save(Order order) {
        try {
            store.put(order.getId(), order);
        } catch (Exception e) {
            throw new InfrastructureException("Failed to save order: " + order.getId(), e);
        }
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
