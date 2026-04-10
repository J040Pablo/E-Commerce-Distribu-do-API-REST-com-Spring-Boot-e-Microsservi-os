package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.PaymentApprovedEvent;
import com.ecommerce.product_service.repository.ProductRepository;
import java.util.List;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApprovedListener {

    private final ProductRepository productRepository;

    public PaymentApprovedListener(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @RabbitListener(queues = "product.stock.queue")
    @Transactional
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        if (event == null || event.getItems() == null || event.getItems().isEmpty()) {
            return;
        }

        if (!"APPROVED".equalsIgnoreCase(event.getStatus())) {
            return;
        }

        List<PaymentApprovedEvent.OrderItemPayload> items = event.getItems();
        for (PaymentApprovedEvent.OrderItemPayload item : items) {
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
}