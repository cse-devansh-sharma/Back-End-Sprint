package com.cap.leave.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Leave-service RabbitMQ config.
 * Does NOT declare exchanges — admin-service owns the topology.
 * Only configures JSON converter and RabbitTemplate for publishing.
 */
@Configuration
public class RabbitMQConfig {

    // Queue names this service listens to
    public static final String LEAVE_APPROVE_COMMAND_QUEUE = "leave.approve.command";
    public static final String LEAVE_REJECT_COMMAND_QUEUE  = "leave.reject.command";

    // Exchange names for publishing
    public static final String LEAVE_EVENTS_EXCHANGE       = "leave.events";
    public static final String NOTIFICATION_EVENTS_EXCHANGE = "notification.events";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
