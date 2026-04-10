package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.PaymentApprovedEvent;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusListener {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = "order.payment-status.queue")
    @Transactional
    public void handlePaymentStatus(PaymentApprovedEvent event) {
        if (event == null || event.getOrderId() == null || event.getStatus() == null) {
            return;
        }

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            applyStatus(order, event.getStatus());
            orderRepository.save(order);
            log.info("Order {} updated from payment event status={}", order.getId(), event.getStatus());
        });
    }

    private void applyStatus(Order order, String paymentStatus) {
        if ("APPROVED".equalsIgnoreCase(paymentStatus)) {
            order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
            return;
        }

        if ("REJECTED".equalsIgnoreCase(paymentStatus)) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
        }
    }
}
