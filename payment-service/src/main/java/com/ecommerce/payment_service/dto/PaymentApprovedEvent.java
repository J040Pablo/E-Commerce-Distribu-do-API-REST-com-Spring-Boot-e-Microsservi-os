package com.ecommerce.payment_service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApprovedEvent {
    private Long orderId;
    private String status;
    private List<OrderItemPayload> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemPayload {
        private Long productId;
        private Integer quantity;
    }
}
