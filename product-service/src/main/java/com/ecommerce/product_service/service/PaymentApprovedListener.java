package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.OrderResponse;
import com.ecommerce.product_service.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentApprovedListener {

    private final RestTemplate restTemplate;
    private final ProductRepository productRepository;

    public PaymentApprovedListener(RestTemplate restTemplate, ProductRepository productRepository) {
        this.restTemplate = restTemplate;
        this.productRepository = productRepository;
    }

    @RabbitListener(queues = "product.stock.queue")
    @Transactional
    public void handlePaymentApproved(Map<String, Object> payload) {
        Long orderId = extractOrderId(payload);
        if (orderId == null) {
            return;
        }

        OrderResponse order = restTemplate.getForObject(
                "http://order-service/api/orders/{id}",
                OrderResponse.class,
                orderId
        );

        if (order == null || order.getItems() == null) {
            return;
        }

        List<OrderResponse.OrderItemResponse> items = order.getItems();
        for (OrderResponse.OrderItemResponse item : items) {
            if (item.getProductId() == null || item.getQuantity() == null) {
                continue;
            }

            productRepository.findById(item.getProductId()).ifPresent(product -> {
                Integer current = product.getQuantity() == null ? 0 : product.getQuantity();
                int updated = Math.max(0, current - item.getQuantity());
                product.setQuantity(updated);
                productRepository.save(product);
            });
        }
    }

    private Long extractOrderId(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object raw = payload.get("orderId");
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong((String) raw);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}