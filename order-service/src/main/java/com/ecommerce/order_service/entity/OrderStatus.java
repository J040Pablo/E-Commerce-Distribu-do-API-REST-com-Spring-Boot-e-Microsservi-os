package com.ecommerce.order_service.entity;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
