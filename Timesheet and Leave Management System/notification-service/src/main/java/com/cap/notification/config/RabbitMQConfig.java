package com.cap.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Notification-service RabbitMQ config.
 * Declares its own queues and binds them to the notification.events topic exchange.
 * Exchanges are declared by admin-service (topology owner) — we use passive if needed.
 */
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EVENTS_EXCHANGE = "notification.events";

    public static final String NOTIF_LEAVE_SUBMITTED   = "notification.leave.submitted";
    public static final String NOTIF_LEAVE_APPROVED    = "notification.leave.approved";
    public static final String NOTIF_LEAVE_REJECTED    = "notification.leave.rejected";
    public static final String NOTIF_TS_SUBMITTED      = "notification.timesheet.submitted";
    public static final String NOTIF_TS_APPROVED       = "notification.timesheet.approved";

    // Declare the exchange (idempotent — safe even if admin already declared it)
    @Bean
    public TopicExchange notificationEventsExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATION_EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean public Queue notifLeaveSubmittedQueue() {
        return QueueBuilder.durable(NOTIF_LEAVE_SUBMITTED).build();
    }
    @Bean public Queue notifLeaveApprovedQueue() {
        return QueueBuilder.durable(NOTIF_LEAVE_APPROVED).build();
    }
    @Bean public Queue notifLeaveRejectedQueue() {
        return QueueBuilder.durable(NOTIF_LEAVE_REJECTED).build();
    }
    @Bean public Queue notifTsSubmittedQueue() {
        return QueueBuilder.durable(NOTIF_TS_SUBMITTED).build();
    }
    @Bean public Queue notifTsApprovedQueue() {
        return QueueBuilder.durable(NOTIF_TS_APPROVED).build();
    }

    @Bean public Binding bindNotifLeaveSubmitted() {
        return BindingBuilder.bind(notifLeaveSubmittedQueue())
                .to(notificationEventsExchange()).with("leave.submitted");
    }
    @Bean public Binding bindNotifLeaveApproved() {
        return BindingBuilder.bind(notifLeaveApprovedQueue())
                .to(notificationEventsExchange()).with("leave.approved");
    }
    @Bean public Binding bindNotifLeaveRejected() {
        return BindingBuilder.bind(notifLeaveRejectedQueue())
                .to(notificationEventsExchange()).with("leave.rejected");
    }
    @Bean public Binding bindNotifTsSubmitted() {
        return BindingBuilder.bind(notifTsSubmittedQueue())
                .to(notificationEventsExchange()).with("timesheet.submitted");
    }
    @Bean public Binding bindNotifTsApproved() {
        return BindingBuilder.bind(notifTsApprovedQueue())
                .to(notificationEventsExchange()).with("timesheet.approved");
    }

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
