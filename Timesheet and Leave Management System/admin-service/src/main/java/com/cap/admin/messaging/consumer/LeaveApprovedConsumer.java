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
 * Listens for leave.approved events fired back by leave-service.
 * Updates local approval_queue status for dashboard accuracy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveApprovedConsumer {

    private final ApprovalQueueRepository approvalQueueRepository;

    @RabbitListener(queues = RabbitMQConfig.LEAVE_APPROVED_EVT)
    public void onLeaveApproved(ApproveCommandEvent event) {
        log.info("[ADMIN] Leave {} was {}", event.getReferenceId(), event.getAction());
        approvalQueueRepository
                .findByReferenceIdAndReferenceType(
                        event.getReferenceId(), ReferenceType.LEAVE)
                .ifPresent(q -> {
                    q.setStatus("APPROVE".equals(event.getAction())
                            ? ApprovalStatus.APPROVED
                            : ApprovalStatus.REJECTED);
                    approvalQueueRepository.save(q);
                });
    }
}
