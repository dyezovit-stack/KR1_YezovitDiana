package com.orderprocessing.service;

import com.orderprocessing.domain.Order;
import com.orderprocessing.domain.OrderStatus;
import com.orderprocessing.exception.OrderNotFoundException;
import com.orderprocessing.payment.PaymentMethod;
import com.orderprocessing.processor.OrderProcessorTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // Composition — all dependencies injected
    private final OrderRepository orderRepository;
    private final OrderProcessorTemplate orderProcessor;

    public OrderService(OrderRepository orderRepository,
                        OrderProcessorTemplate orderProcessor) {
        this.orderRepository = orderRepository;
        this.orderProcessor = orderProcessor;
    }

    public Order placeOrder(Order order) {
        log.info("Placing order {}", order.getId());
        orderRepository.save(order);
        return order;
    }

    public void processOrder(String orderId, PaymentMethod paymentMethod) {
        log.info("Processing order {}", orderId);
        Order order = findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        try {
            orderProcessor.process(order, paymentMethod);
            orderRepository.save(order);
            log.info("Order {} successfully processed", orderId);
        } catch (Exception e) {
            log.error("Unexpected error while processing order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Optional API method — findById returns Optional<Order>.
     */
    public Optional<Order> findById(String id) {
        return orderRepository.findById(id);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public void cancelOrder(String orderId) {
        Order order = findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() != OrderStatus.NEW) {
            log.warn("Attempted to cancel order {} in status {}", orderId, order.getStatus());
        }
        order.cancel();
        orderRepository.save(order);
        log.info("Order {} cancelled", orderId);
    }

    public void shipOrder(String orderId) {
        Order order = findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markAsShipped();
        orderRepository.save(order);
        log.info("Order {} marked as SHIPPED", orderId);
    }

    public void deliverOrder(String orderId) {
        Order order = findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markAsDelivered();
        orderRepository.save(order);
        log.info("Order {} marked as DELIVERED", orderId);
    }
}
