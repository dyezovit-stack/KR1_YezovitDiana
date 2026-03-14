package com.orderprocessing.domain;

import com.orderprocessing.domain.vo.Email;
import com.orderprocessing.domain.vo.Money;
import com.orderprocessing.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class Order {

    private static final int MAX_ITEMS = 10;

    private final String id;
    private final Email customerEmail;
    private final OrderItem[] items;
    private final LocalDateTime createdAt;
    private OrderStatus status;
    private Money totalAmount;
    private Money discountedAmount;

    // Primary constructor
    public Order(String id, Email customerEmail, OrderItem[] items) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Order ID must not be blank");
        }
        if (customerEmail == null) {
            throw new IllegalArgumentException("Customer email must not be null");
        }
        if (items == null || items.length == 0) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (items.length > MAX_ITEMS) {
            throw new IllegalArgumentException(
                "Order cannot have more than " + MAX_ITEMS + " items (R1 rule)");
        }
        this.id = id;
        this.customerEmail = customerEmail;
        this.items = Arrays.copyOf(items, items.length); // defensive copy
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.NEW;
        this.totalAmount = calculateRawTotal();
        this.discountedAmount = this.totalAmount;
    }

    // Constructor chaining via this(...) — auto-generate ID
    public Order(Email customerEmail, OrderItem[] items) {
        this(UUID.randomUUID().toString(), customerEmail, items);
    }

    // Convenience constructor for single item
    public Order(Email customerEmail, OrderItem singleItem) {
        this(customerEmail, new OrderItem[]{singleItem});
    }

    private Money calculateRawTotal() {
        Money total = new Money(BigDecimal.ZERO, "UAH");
        for (OrderItem item : items) {
            total = total.add(item.getTotalPrice());
        }
        return total;
    }

    public void applyDiscount(BigDecimal discountRate) {
        BigDecimal multiplier = BigDecimal.ONE.subtract(discountRate);
        this.discountedAmount = totalAmount.multiply(multiplier);
    }

    public void markAsPaid() {
        if (status != OrderStatus.NEW) {
            throw new InvalidOrderStateException(
                "Cannot pay order in status: " + status + ". Expected: NEW");
        }
        this.status = OrderStatus.PAID;
    }

    public void markAsShipped() {
        if (status != OrderStatus.PAID) {
            throw new InvalidOrderStateException(
                "Cannot ship order in status: " + status + ". Expected: PAID");
        }
        this.status = OrderStatus.SHIPPED;
    }

    public void markAsDelivered() {
        if (status != OrderStatus.SHIPPED) {
            throw new InvalidOrderStateException(
                "Cannot deliver order in status: " + status + ". Expected: SHIPPED");
        }
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status != OrderStatus.NEW) {
            throw new InvalidOrderStateException(
                "Cannot cancel order in status: " + status + ". Only NEW orders can be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // Defensive copy for items array
    public OrderItem[] getItems() {
        return Arrays.copyOf(items, items.length);
    }

    public String getId() {
        return id;
    }

    public Email getCustomerEmail() {
        return customerEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Money getDiscountedAmount() {
        return discountedAmount;
    }

    public int getItemCount() {
        return items.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
            "id='" + id + '\'' +
            ", customerEmail=" + customerEmail +
            ", itemCount=" + items.length +
            ", totalAmount=" + totalAmount +
            ", discountedAmount=" + discountedAmount +
            ", status=" + status +
            ", createdAt=" + createdAt +
            '}';
    }
}
