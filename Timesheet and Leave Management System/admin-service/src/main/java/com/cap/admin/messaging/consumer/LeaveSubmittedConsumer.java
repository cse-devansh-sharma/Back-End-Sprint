package com.cap.admin.messaging.consumer;

import com.cap.admin.config.RabbitMQConfig;
import com.cap.admin.enums.ReferenceType;
import com.cap.admin.messaging.dto.LeaveSubmittedEvent;
import com.cap.admin.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for leave.submitted events published by leave-service.
 * Inserts a new record into the approval_queue automatically.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveSubmittedConsumer {

    private final ApprovalService approvalService;

    @RabbitListener(queues = RabbitMQConfig.LEAVE_SUBMITTED_QUEUE)
    public void onLeaveSubmitted(LeaveSubmittedEvent event) {
        log.info("[ADMIN] Received leave.submitted for leaveId={}, userId={}",
                event.getLeaveRequestId(), event.getUserId());
        try {
            approvalService.addToQueue(
                    event.getLeaveRequestId(),
                    ReferenceType.LEAVE,
                    event.getUserId(),
                    event.getManagerId()
            );
            log.info("[ADMIN] Enqueued leave {} for manager {}", event.getLeaveRequestId(), event.getManagerId());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to enqueue leave {}: {}", event.getLeaveRequestId(), e.getMessage());
            throw e;
        }
    }
}
