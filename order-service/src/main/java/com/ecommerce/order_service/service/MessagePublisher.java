package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String PAYMENT_ROUTING_KEY = "payment.routing.key";

    public void publishPaymentEvent(PaymentEvent event) {
        log.info("Publishing payment event for order: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(ORDER_EXCHANGE, PAYMENT_ROUTING_KEY, event);
        log.info("Payment event published successfully");
    }
}
