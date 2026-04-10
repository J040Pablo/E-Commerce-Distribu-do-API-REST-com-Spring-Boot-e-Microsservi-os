package com.ecommerce.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private List<OrderItemPayload> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemPayload {
        private Long productId;
        private Integer quantity;
    }
}
