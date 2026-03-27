package com.cap.notification.messaging.consumer;

import com.cap.notification.config.RabbitMQConfig;
import com.cap.notification.dto.NotificationEvent;
import com.cap.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Single consumer for all notification events across the system.
 * Saves each notification to the DB for the target user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIF_LEAVE_SUBMITTED)
    public void onLeaveSubmitted(NotificationEvent event) {
        log.info("[NOTIF] leave.submitted for userId={}", event.getUserId());
        notificationService.saveNotification(
                event.getUserId(),
                "LEAVE_SUBMITTED",
                event.getTitle() != null ? event.getTitle() : "Leave Request Submitted",
                event.getBody() != null ? event.getBody() : "Your leave request has been submitted for approval.");
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIF_LEAVE_APPROVED)
    public void onLeaveApproved(NotificationEvent event) {
        log.info("[NOTIF] leave.approved for userId={}", event.getUserId());
        notificationService.saveNotification(
                event.getUserId(),
                "LEAVE_APPROVED",
                event.getTitle() != null ? event.getTitle() : "Leave Request Approved",
                event.getBody() != null ? event.getBody() : "Your leave request has been approved.");
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIF_LEAVE_REJECTED)
    public void onLeaveRejected(NotificationEvent event) {
        log.info("[NOTIF] leave.rejected for userId={}", event.getUserId());
        notificationService.saveNotification(
                event.getUserId(),
                "LEAVE_REJECTED",
                event.getTitle() != null ? event.getTitle() : "Leave Request Rejected",
                event.getBody() != null ? event.getBody() : "Your leave request was rejected.");
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIF_TS_SUBMITTED)
    public void onTimesheetSubmitted(NotificationEvent event) {
        log.info("[NOTIF] timesheet.submitted for userId={}", event.getUserId());
        notificationService.saveNotification(
                event.getUserId(),
                "TIMESHEET_SUBMITTED",
                event.getTitle() != null ? event.getTitle() : "Timesheet Submitted",
                event.getBody() != null ? event.getBody() : "Your timesheet has been submitted for approval.");
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIF_TS_APPROVED)
    public void onTimesheetApproved(NotificationEvent event) {
        log.info("[NOTIF] timesheet.approved for userId={}", event.getUserId());
        notificationService.saveNotification(
                event.getUserId(),
                "TIMESHEET_APPROVED",
                event.getTitle() != null ? event.getTitle() : "Timesheet Approved",
                event.getBody() != null ? event.getBody() : "Your timesheet has been approved.");
    }
}
