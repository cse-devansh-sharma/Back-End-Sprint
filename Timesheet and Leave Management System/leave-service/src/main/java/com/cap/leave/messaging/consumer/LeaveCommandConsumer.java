package com.cap.leave.messaging.consumer;

import com.cap.leave.config.RabbitMQConfig;
import com.cap.leave.messaging.dto.ApproveCommandEvent;
import com.cap.leave.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens on admin.commands exchange for approve/reject commands targeting leave requests.
 * Delegates to LeaveService.approveLeave() or rejectLeave().
 * Leave-service is fully independent — reacts only to RabbitMQ messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveCommandConsumer {

    private final LeaveService leaveService;

    @RabbitListener(queues = RabbitMQConfig.LEAVE_APPROVE_COMMAND_QUEUE)
    public void onApproveCommand(ApproveCommandEvent event) {
        log.info("[LEAVE] Received approve command for leaveId={}", event.getReferenceId());
        try {
            leaveService.approveLeave(
                    event.getReferenceId(),
                    event.getApproverId(),
                    event.getRemark());
            log.info("[LEAVE] Leave {} approved by manager {}", event.getReferenceId(), event.getApproverId());
        } catch (Exception e) {
            log.error("[LEAVE] Failed to approve leave {}: {}", event.getReferenceId(), e.getMessage());
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.LEAVE_REJECT_COMMAND_QUEUE)
    public void onRejectCommand(ApproveCommandEvent event) {
        log.info("[LEAVE] Received reject command for leaveId={}", event.getReferenceId());
        try {
            leaveService.rejectLeave(
                    event.getReferenceId(),
                    event.getApproverId(),
                    event.getRemark());
            log.info("[LEAVE] Leave {} rejected", event.getReferenceId());
        } catch (Exception e) {
            log.error("[LEAVE] Failed to reject leave {}: {}", event.getReferenceId(), e.getMessage());
            throw e;
        }
    }
}
