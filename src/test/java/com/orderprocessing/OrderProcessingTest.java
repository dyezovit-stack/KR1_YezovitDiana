package com.orderprocessing;

import com.orderprocessing.domain.Order;
import com.orderprocessing.domain.OrderItem;
import com.orderprocessing.domain.OrderStatus;
import com.orderprocessing.domain.vo.Email;
import com.orderprocessing.domain.vo.Money;
import com.orderprocessing.exception.*;
import com.orderprocessing.payment.BankTransferPayment;
import com.orderprocessing.payment.CardPayment;
import com.orderprocessing.payment.PayPalPayment;
import com.orderprocessing.processor.StandardOrderProcessor;
import com.orderprocessing.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OrderProcessingTest {

    private InMemoryStockRepository stockRepository;
    private NotificationService notificationService;
    private StandardOrderProcessor processor;
    private OrderRepository orderRepository;
    private OrderService orderService;

    private static final Email CUSTOMER = new Email("customer@example.com");

    @BeforeEach
    void setUp() {
        stockRepository = new InMemoryStockRepository();
        notificationService = new LoggingNotificationService();
        processor = new StandardOrderProcessor(stockRepository, notificationService);
        orderRepository = new InMemoryOrderRepository();
        orderService = new OrderService(orderRepository, processor);
    }

    // ─── POSITIVE TESTS ────────────────────────────────────────────────────────

    @Test
    @DisplayName("T01 - Place and find order by ID")
    void testPlaceAndFindOrder() {
        stockRepository.addStock("P1", 10);
        Order order = new Order(CUSTOMER, new OrderItem("P1", "Laptop", 1, new Money(5000, "UAH")));
        orderService.placeOrder(order);

        Optional<Order> found = orderService.findById(order.getId());
        assertTrue(found.isPresent());
        assertEquals(order.getId(), found.get().getId());
    }

    @Test
    @DisplayName("T02 - Full order lifecycle: NEW -> PAID -> SHIPPED -> DELIVERED")
    void testFullOrderLifecycle() {
        stockRepository.addStock("P1", 5);
        Order order = new Order(CUSTOMER, new OrderItem("P1", "Phone", 1, new Money(3000, "UAH")));
        orderService.placeOrder(order);

        CardPayment card = new CardPayment("1234");
        orderService.processOrder(order.getId(), card);
        assertEquals(OrderStatus.PAID, order.getStatus());

        orderService.shipOrder(order.getId());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());

        orderService.deliverOrder(order.getId());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    @DisplayName("T03 - Cancel order in NEW state")
    void testCancelNewOrder() {
        Order order = new Order(CUSTOMER, new OrderItem("P1", "Book", 1, new Money(200, "UAH")));
        orderService.placeOrder(order);
        orderService.cancelOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("T04 - Discount 5% applied when total >= 10_000 UAH")
    void testDiscountApplied() {
        stockRepository.addStock("P1", 5);
        Order order = new Order(CUSTOMER, new OrderItem("P1", "TV", 1, new Money(12000, "UAH")));
        orderService.placeOrder(order);
        orderService.processOrder(order.getId(), new CardPayment("0000"));

        Money expected = new Money(new BigDecimal("11400.00"), "UAH");
        assertEquals(expected, order.getDiscountedAmount());
    }

    @Test
    @DisplayName("T05 - No discount when total < 10_000 UAH")
    void testNoDiscount() {
        stockRepository.addStock("P1", 5);
        Order order = new Order(CUSTOMER, new OrderItem("P1", "Notebook", 2, new Money(1000, "UAH")));
        orderService.placeOrder(order);
        orderService.processOrder(order.getId(), new CardPayment("5678"));

        assertEquals(order.getTotalAmount(), order.getDiscountedAmount());
    }

    @Test
    @DisplayName("T06 - BankTransfer commission 2% for amount < 50_000")
    void testBankTransferLowCommission() {
        BankTransferPayment bank = new BankTransferPayment("UA123456");
        Money amount = new Money(10000, "UAH");
        Money charged = bank.processPayment(amount);
        assertEquals(new Money(new BigDecimal("10200.00"), "UAH"), charged);
    }

    @Test
    @DisplayName("T07 - BankTransfer commission 0.5% for amount >= 50_000")
    void testBankTransferHighCommission() {
        BankTransferPayment bank = new BankTransferPayment("UA123456");
        Money amount = new Money(50000, "UAH");
        Money charged = bank.processPayment(amount);
        assertEquals(new Money(new BigDecimal("50250.00"), "UAH"), charged);
    }

    @Test
    @DisplayName("T08 - Money equals and hashCode")
    void testMoneyEqualsHashCode() {
        Money m1 = new Money(new BigDecimal("100.00"), "UAH");
        Money m2 = new Money(new BigDecimal("100.00"), "UAH");
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    @DisplayName("T09 - Order constructor chaining (auto-generated ID)")
    void testOrderAutoId() {
        OrderItem item = new OrderItem("P1", "Item", 1, new Money(100, "UAH"));
        Order order = new Order(CUSTOMER, item);
        assertNotNull(order.getId());
        assertFalse(order.getId().isBlank());
    }

    @Test
    @DisplayName("T10 - Defensive copy: modifying returned items array does not affect order")
    void testDefensiveCopy() {
        OrderItem item = new OrderItem("P1", "Widget", 1, new Money(500, "UAH"));
        Order order = new Order(CUSTOMER, new OrderItem[]{item});
        OrderItem[] copy = order.getItems();
        copy[0] = null; // mutate the copy
        assertNotNull(order.getItems()[0]); // original unchanged
    }

    @Test
    @DisplayName("T11 - findById returns empty Optional for unknown ID")
    void testFindByIdEmpty() {
        Optional<Order> result = orderService.findById("nonexistent-id");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("T12 - PayPalPayment processes amount above minimum")
    void testPayPalSuccess() {
        PayPalPayment paypal = new PayPalPayment("user@paypal.com");
        Money amount = new Money(1000, "UAH");
        Money result = paypal.processPayment(amount);
        assertEquals(amount, result);
    }

    // ─── PARAMETRIZED TEST ─────────────────────────────────────────────────────

    @ParameterizedTest(name = "T13[{index}] - CardPayment valid for {0} UAH")
    @ValueSource(doubles = {100.0, 1000.0, 9999.99, 44999.99, 45000.0})
    @DisplayName("T13 - CardPayment accepts amounts up to 45_000")
    void testCardPaymentValidAmounts(double amount) {
        CardPayment card = new CardPayment("1111");
        Money money = new Money(amount, "UAH");
        assertDoesNotThrow(() -> card.validate(money));
    }

    @ParameterizedTest(name = "T14[{index}] - Order with {0} items")
    @CsvSource({"1,100", "5,200", "10,150"})
    @DisplayName("T14 - Orders with 1–10 items are valid")
    void testOrderItemCountBoundary(int count, double price) {
        OrderItem[] items = new OrderItem[count];
        for (int i = 0; i < count; i++) {
            stockRepository.addStock("P" + i, 10);
            items[i] = new OrderItem("P" + i, "Product" + i, 1, new Money(price, "UAH"));
        }
        Order order = new Order(CUSTOMER, items);
        orderService.placeOrder(order);
        assertEquals(count, order.getItemCount());
    }

    // ─── NEGATIVE TESTS ────────────────────────────────────────────────────────

    @Test
    @DisplayName("N01 - OutOfStockException when product unavailable")
    void testOutOfStock() {
        // NO stock added for P1
        Order order = new Order(CUSTOMER, new OrderItem("P1", "Gadget", 1, new Money(500, "UAH")));
        orderService.placeOrder(order);

        OutOfStockException ex = assertThrows(OutOfStockException.class,
            () -> orderService.processOrder(order.getId(), new CardPayment("9999")));
        assertEquals("P1", ex.getProductId());
    }

    @Test
    @DisplayName("N02 - ValidationException when order has more than 10 items (R1 rule)")
    void testTooManyItems() {
        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem[] items = new OrderItem[11];
            for (int i = 0; i < 11; i++) {
                items[i] = new OrderItem("P" + i, "Item" + i, 1, new Money(100, "UAH"));
            }
            new Order(CUSTOMER, items);
        });
    }

    @Test
    @DisplayName("N03 - PaymentException when CardPayment exceeds 45_000")
    void testCardPaymentExceedsLimit() {
        CardPayment card = new CardPayment("2222");
        Money over = new Money(45_001, "UAH");
        PaymentException ex = assertThrows(PaymentException.class,
            () -> card.processPayment(over));
        assertTrue(ex.getMessage().contains("45000"));
    }

    @Test
    @DisplayName("N04 - PaymentException when PayPalPayment below 600")
    void testPayPalBelowMinimum() {
        PayPalPayment paypal = new PayPalPayment("user@paypal.com");
        Money under = new Money(599, "UAH");
        assertThrows(PaymentException.class, () -> paypal.processPayment(under));
    }

    @Test
    @DisplayName("N05 - InvalidOrderStateException when cancelling non-NEW order")
    void testCancelPaidOrderFails() {
        stockRepository.addStock("P1", 5);
        Order order = new Order(CUSTOMER, new OrderItem("P1", "Camera", 1, new Money(3000, "UAH")));
        orderService.placeOrder(order);
        orderService.processOrder(order.getId(), new CardPayment("3333"));

        // order is now PAID — cancel should throw
        assertThrows(InvalidOrderStateException.class,
            () -> orderService.cancelOrder(order.getId()));
    }

    @Test
    @DisplayName("N06 - OrderNotFoundException when processing unknown order")
    void testProcessUnknownOrder() {
        assertThrows(OrderNotFoundException.class,
            () -> orderService.processOrder("ghost-id", new CardPayment("4444")));
    }

    @Test
    @DisplayName("N07 - InvalidOrderStateException: ship before pay")
    void testShipBeforePay() {
        Order order = new Order(CUSTOMER, new OrderItem("P1", "Desk", 1, new Money(800, "UAH")));
        orderService.placeOrder(order);
        assertThrows(InvalidOrderStateException.class,
            () -> orderService.shipOrder(order.getId()));
    }

    @Test
    @DisplayName("N08 - Invalid email throws IllegalArgumentException")
    void testInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
    }

    @Test
    @DisplayName("N09 - Money with negative amount throws IllegalArgumentException")
    void testNegativeMoney() {
        assertThrows(IllegalArgumentException.class, () -> new Money(-1.0, "UAH"));
    }

    @Test
    @DisplayName("N10 - InvalidOrderStateException: deliver before ship")
    void testDeliverBeforeShip() {
        stockRepository.addStock("P1", 5);
        Order order = new Order(CUSTOMER, new OrderItem("P1", "Chair", 1, new Money(2000, "UAH")));
        orderService.placeOrder(order);
        orderService.processOrder(order.getId(), new CardPayment("5555"));
        // PAID but not SHIPPED — deliver should throw
        assertThrows(InvalidOrderStateException.class,
            () -> orderService.deliverOrder(order.getId()));
    }
}
