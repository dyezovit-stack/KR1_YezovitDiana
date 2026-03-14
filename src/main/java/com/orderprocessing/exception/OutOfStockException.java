package com.orderprocessing.exception;

public class OutOfStockException extends AppException {

    private final String productId;

    public OutOfStockException(String productId) {
        super("Product is out of stock: " + productId);
        this.productId = productId;
    }

    public OutOfStockException(String productId, Throwable cause) {
        super("Product is out of stock: " + productId, cause);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
