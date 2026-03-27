package com.cap.admin.messaging.consumer;

import com.cap.admin.config.RabbitMQConfig;
import com.cap.admin.enums.ReferenceType;
import com.cap.admin.messaging.dto.TimesheetSubmittedEvent;
import com.cap.admin.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for timesheet.submitted events published by timesheet-service.
 * Inserts a new record into the approval_queue automatically.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TimesheetSubmittedConsumer {

    private final ApprovalService approvalService;

    @RabbitListener(queues = RabbitMQConfig.TIMESHEET_SUBMITTED_QUEUE)
    public void onTimesheetSubmitted(TimesheetSubmittedEvent event) {
        log.info("[ADMIN] Received timesheet.submitted for timesheetId={}, userId={}",
                event.getTimesheetId(), event.getUserId());
        try {
            approvalService.addToQueue(
                    event.getTimesheetId(),
                    ReferenceType.TIMESHEET,
                    event.getUserId(),
                    event.getManagerId()
            );
            log.info("[ADMIN] Enqueued timesheet {} for manager {}", event.getTimesheetId(), event.getManagerId());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to enqueue timesheet {}: {}", event.getTimesheetId(), e.getMessage());
            throw e; // re-throw so RabbitMQ can retry / DLQ
        }
    }
}
