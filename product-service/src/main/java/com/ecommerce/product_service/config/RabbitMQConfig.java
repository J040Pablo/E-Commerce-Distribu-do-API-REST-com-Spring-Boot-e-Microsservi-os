package com.ecommerce.product_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String PAYMENT_APPROVED_QUEUE = "product.stock.queue";
    private static final String PAYMENT_APPROVED_ROUTING_KEY = "payment.approved";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return new Queue(PAYMENT_APPROVED_QUEUE, true);
    }

    @Bean
    public Binding paymentApprovedBinding() {
        return BindingBuilder
                .bind(paymentApprovedQueue())
                .to(orderExchange())
                .with(PAYMENT_APPROVED_ROUTING_KEY);
    }
}