package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.PaymentEvent;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.entity.PaymentStatus;
import com.ecommerce.payment_service.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentListener {

    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String PAYMENT_APPROVED_ROUTING_KEY = "payment.approved";

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "payment.queue")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event for order: {}", event.getOrderId());

        try {
            Payment payment = new Payment();
            payment.setOrderId(event.getOrderId());
            payment.setCustomerId(event.getCustomerId());
            payment.setAmount(event.getAmount());
            payment.setPaymentMethod(event.getPaymentMethod());
            payment.setStatus(PaymentStatus.PROCESSING);

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment record created with ID: {}", savedPayment.getId());

            boolean paymentApproved = paymentService.processPayment(savedPayment);

            if (paymentApproved) {
                savedPayment.setStatus(PaymentStatus.APPROVED);
                savedPayment.setTransactionId(UUID.randomUUID().toString());
                savedPayment.setProcessedAt(LocalDateTime.now());
                log.info("Payment approved for order: {}", event.getOrderId());
            } else {
                savedPayment.setStatus(PaymentStatus.REJECTED);
                savedPayment.setProcessedAt(LocalDateTime.now());
                log.warn("Payment rejected for order: {}", event.getOrderId());
            }

            paymentRepository.save(savedPayment);

            if (savedPayment.getStatus() == PaymentStatus.APPROVED) {
                rabbitTemplate.convertAndSend(
                        ORDER_EXCHANGE,
                        PAYMENT_APPROVED_ROUTING_KEY,
                        Map.of("orderId", savedPayment.getOrderId())
                );
            }

        } catch (Exception e) {
            log.error("Error processing payment for order: {}", event.getOrderId(), e);
        }
    }
}
