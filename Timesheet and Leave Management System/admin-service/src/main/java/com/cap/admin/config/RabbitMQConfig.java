package com.cap.admin.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Admin-service is the TOPOLOGY OWNER.
 * It declares all exchanges, queues, and bindings for the entire system.
 * Other services only declare the queues they listen to.
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange Names ──────────────────────────────────────────
    public static final String TIMESHEET_EVENTS_EXCHANGE  = "timesheet.events";
    public static final String LEAVE_EVENTS_EXCHANGE      = "leave.events";
    public static final String ADMIN_COMMANDS_EXCHANGE    = "admin.commands";
    public static final String MASTER_DATA_SYNC_EXCHANGE  = "master.data.sync";
    public static final String NOTIFICATION_EVENTS_EXCHANGE = "notification.events";

    // ── Queue Names ─────────────────────────────────────────────
    // Admin listens
    public static final String TIMESHEET_SUBMITTED_QUEUE   = "timesheet.submitted";
    public static final String LEAVE_SUBMITTED_QUEUE       = "leave.submitted";
    public static final String TIMESHEET_APPROVED_EVT      = "admin.timesheet.approved";
    public static final String LEAVE_APPROVED_EVT          = "admin.leave.approved";

    // Timesheet-service listens
    public static final String TS_APPROVE_COMMAND_QUEUE    = "timesheet.approve.command";
    public static final String TS_REJECT_COMMAND_QUEUE     = "timesheet.reject.command";

    // Leave-service listens
    public static final String LEAVE_APPROVE_COMMAND_QUEUE = "leave.approve.command";
    public static final String LEAVE_REJECT_COMMAND_QUEUE  = "leave.reject.command";

    // Notification-service listens
    public static final String NOTIF_LEAVE_SUBMITTED   = "notification.leave.submitted";
    public static final String NOTIF_LEAVE_APPROVED    = "notification.leave.approved";
    public static final String NOTIF_LEAVE_REJECTED    = "notification.leave.rejected";
    public static final String NOTIF_TS_SUBMITTED      = "notification.timesheet.submitted";
    public static final String NOTIF_TS_APPROVED       = "notification.timesheet.approved";

    // ── Routing Keys ────────────────────────────────────────────
    public static final String RK_TIMESHEET_SUBMITTED  = "timesheet.submitted";
    public static final String RK_LEAVE_SUBMITTED      = "leave.submitted";
    public static final String RK_TIMESHEET_APPROVED   = "timesheet.approved";
    public static final String RK_LEAVE_APPROVED       = "leave.approved";
    public static final String RK_TS_APPROVE_CMD       = "timesheet.approve.command";
    public static final String RK_TS_REJECT_CMD        = "timesheet.reject.command";
    public static final String RK_LEAVE_APPROVE_CMD    = "leave.approve.command";
    public static final String RK_LEAVE_REJECT_CMD     = "leave.reject.command";

    // ────────────────────────────────────────────────────────────
    // EXCHANGES
    // ────────────────────────────────────────────────────────────

    @Bean public TopicExchange timesheetEventsExchange() {
        return ExchangeBuilder.topicExchange(TIMESHEET_EVENTS_EXCHANGE).durable(true).build();
    }
    @Bean public TopicExchange leaveEventsExchange() {
        return ExchangeBuilder.topicExchange(LEAVE_EVENTS_EXCHANGE).durable(true).build();
    }
    @Bean public DirectExchange adminCommandsExchange() {
        return ExchangeBuilder.directExchange(ADMIN_COMMANDS_EXCHANGE).durable(true).build();
    }
    @Bean public FanoutExchange masterDataSyncExchange() {
        return ExchangeBuilder.fanoutExchange(MASTER_DATA_SYNC_EXCHANGE).durable(true).build();
    }
    @Bean public TopicExchange notificationEventsExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATION_EVENTS_EXCHANGE).durable(true).build();
    }

    // ────────────────────────────────────────────────────────────
    // QUEUES (all durable, with Dead Letter Exchange)
    // ────────────────────────────────────────────────────────────

    @Bean public Queue timesheetSubmittedQueue() {
        return QueueBuilder.durable(TIMESHEET_SUBMITTED_QUEUE).build();
    }
    @Bean public Queue leaveSubmittedQueue() {
        return QueueBuilder.durable(LEAVE_SUBMITTED_QUEUE).build();
    }
    @Bean public Queue adminTimesheetApprovedQueue() {
        return QueueBuilder.durable(TIMESHEET_APPROVED_EVT).build();
    }
    @Bean public Queue adminLeaveApprovedQueue() {
        return QueueBuilder.durable(LEAVE_APPROVED_EVT).build();
    }
    @Bean public Queue tsApproveCommandQueue() {
        return QueueBuilder.durable(TS_APPROVE_COMMAND_QUEUE).build();
    }
    @Bean public Queue tsRejectCommandQueue() {
        return QueueBuilder.durable(TS_REJECT_COMMAND_QUEUE).build();
    }
    @Bean public Queue leaveApproveCommandQueue() {
        return QueueBuilder.durable(LEAVE_APPROVE_COMMAND_QUEUE).build();
    }
    @Bean public Queue leaveRejectCommandQueue() {
        return QueueBuilder.durable(LEAVE_REJECT_COMMAND_QUEUE).build();
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
    @Bean public Queue projectSyncTimesheetQueue() {
        return QueueBuilder.durable("master.data.sync.timesheet").build();
    }

    // ────────────────────────────────────────────────────────────
    // BINDINGS
    // ────────────────────────────────────────────────────────────

    // timesheet.events → admin queue
    @Bean public Binding bindTimesheetSubmittedToAdmin() {
        return BindingBuilder.bind(timesheetSubmittedQueue())
                .to(timesheetEventsExchange()).with(RK_TIMESHEET_SUBMITTED);
    }
    // leave.events → admin queue
    @Bean public Binding bindLeaveSubmittedToAdmin() {
        return BindingBuilder.bind(leaveSubmittedQueue())
                .to(leaveEventsExchange()).with(RK_LEAVE_SUBMITTED);
    }
    // timesheet.events → admin.timesheet.approved queue
    @Bean public Binding bindTimesheetApprovedToAdmin() {
        return BindingBuilder.bind(adminTimesheetApprovedQueue())
                .to(timesheetEventsExchange()).with(RK_TIMESHEET_APPROVED);
    }
    // leave.events → admin.leave.approved queue
    @Bean public Binding bindLeaveApprovedToAdmin() {
        return BindingBuilder.bind(adminLeaveApprovedQueue())
                .to(leaveEventsExchange()).with(RK_LEAVE_APPROVED);
    }
    // admin.commands → timesheet approve/reject queues
    @Bean public Binding bindTsApproveCommand() {
        return BindingBuilder.bind(tsApproveCommandQueue())
                .to(adminCommandsExchange()).with(RK_TS_APPROVE_CMD);
    }
    @Bean public Binding bindTsRejectCommand() {
        return BindingBuilder.bind(tsRejectCommandQueue())
                .to(adminCommandsExchange()).with(RK_TS_REJECT_CMD);
    }
    // admin.commands → leave approve/reject queues
    @Bean public Binding bindLeaveApproveCommand() {
        return BindingBuilder.bind(leaveApproveCommandQueue())
                .to(adminCommandsExchange()).with(RK_LEAVE_APPROVE_CMD);
    }
    @Bean public Binding bindLeaveRejectCommand() {
        return BindingBuilder.bind(leaveRejectCommandQueue())
                .to(adminCommandsExchange()).with(RK_LEAVE_REJECT_CMD);
    }
    // notification.events → notification queues
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
    @Bean public Binding bindProjectSyncTimesheet() {
        return BindingBuilder.bind(projectSyncTimesheetQueue())
                .to(masterDataSyncExchange());
    }

    // ────────────────────────────────────────────────────────────
    // JSON Message Converter + RabbitTemplate
    // ────────────────────────────────────────────────────────────

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
