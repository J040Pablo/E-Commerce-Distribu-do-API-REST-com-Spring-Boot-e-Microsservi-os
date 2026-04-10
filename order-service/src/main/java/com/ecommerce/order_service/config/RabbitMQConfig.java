package com.ecommerce.order_service.config;

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
    private static final String PAYMENT_QUEUE = "payment.queue";
    private static final String PAYMENT_ROUTING_KEY = "payment.routing.key";
    private static final String PAYMENT_STATUS_QUEUE = "order.payment-status.queue";
    private static final String PAYMENT_STATUS_ROUTING_KEY = "payment.status";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(orderExchange())
                .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Queue paymentStatusQueue() {
        return new Queue(PAYMENT_STATUS_QUEUE, true);
    }

    @Bean
    public Binding paymentStatusBinding() {
        return BindingBuilder
                .bind(paymentStatusQueue())
                .to(orderExchange())
                .with(PAYMENT_STATUS_ROUTING_KEY);
    }
}
