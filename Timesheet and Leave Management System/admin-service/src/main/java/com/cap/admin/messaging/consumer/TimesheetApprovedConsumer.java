package com.cap.admin.messaging.consumer;

import com.cap.admin.config.RabbitMQConfig;
import com.cap.admin.enums.ApprovalStatus;
import com.cap.admin.enums.ReferenceType;
import com.cap.admin.messaging.dto.ApproveCommandEvent;
import com.cap.admin.repository.ApprovalQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for timesheet.approved events fired back by timesheet-service.
 * Updates local approval_queue status for dashboard accuracy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TimesheetApprovedConsumer {

    private final ApprovalQueueRepository approvalQueueRepository;

    @RabbitListener(queues = RabbitMQConfig.TIMESHEET_APPROVED_EVT)
    public void onTimesheetApproved(ApproveCommandEvent event) {
        log.info("[ADMIN] Timesheet {} was {}", event.getReferenceId(), event.getAction());
        approvalQueueRepository
                .findByReferenceIdAndReferenceType(
                        event.getReferenceId(), ReferenceType.TIMESHEET)
                .ifPresent(q -> {
                    q.setStatus("APPROVE".equals(event.getAction())
                            ? ApprovalStatus.APPROVED
                            : ApprovalStatus.REJECTED);
                    approvalQueueRepository.save(q);
                });
    }
}
