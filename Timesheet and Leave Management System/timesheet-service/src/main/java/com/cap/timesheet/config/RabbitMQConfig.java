package com.cap.timesheet.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Timesheet-service RabbitMQ config.
 * Does NOT declare exchanges — admin-service owns the topology.
 * Only configures the JSON converter and RabbitTemplate for publishing.
 */
@Configuration
public class RabbitMQConfig {

    // Queue names this service listens to (declared in admin-service)
    public static final String TS_APPROVE_COMMAND_QUEUE = "timesheet.approve.command";
    public static final String TS_REJECT_COMMAND_QUEUE  = "timesheet.reject.command";

    // Exchange names for publishing
    public static final String TIMESHEET_EVENTS_EXCHANGE   = "timesheet.events";
    public static final String NOTIFICATION_EVENTS_EXCHANGE = "notification.events";

    // Routing keys for publishing
    public static final String RK_SUBMITTED = "timesheet.submitted";
    public static final String RK_APPROVED  = "timesheet.approved";

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
