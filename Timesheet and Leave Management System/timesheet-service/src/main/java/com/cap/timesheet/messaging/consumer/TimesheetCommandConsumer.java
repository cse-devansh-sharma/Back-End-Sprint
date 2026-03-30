package com.cap.timesheet.messaging.consumer;

import com.cap.timesheet.config.RabbitMQConfig;
import com.cap.timesheet.messaging.dto.ApproveCommandEvent;
import com.cap.timesheet.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class TimesheetCommandConsumer {

    private final TimesheetService timesheetService;

    @RabbitListener(queues = RabbitMQConfig.TS_APPROVE_COMMAND_QUEUE)
    public void onApproveCommand(ApproveCommandEvent event) {
        log.info("[TIMESHEET] Received approve command for timesheetId={}", event.getReferenceId());
        try {
            timesheetService.approveTimesheet(
                    event.getReferenceId(),
                    event.getApproverId(),
                    event.getRemark());
            log.info("[TIMESHEET] Timesheet {} approved by manager {}", event.getReferenceId(), event.getApproverId());
        } catch (Exception e) {
            log.error("[TIMESHEET] Failed to approve timesheet {}: {}", event.getReferenceId(), e.getMessage());
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.TS_REJECT_COMMAND_QUEUE)
    public void onRejectCommand(ApproveCommandEvent event) {
        log.info("[TIMESHEET] Received reject command for timesheetId={}", event.getReferenceId());
        try {
            timesheetService.rejectTimesheet(
                    event.getReferenceId(),
                    event.getApproverId(),
                    event.getRemark());
            log.info("[TIMESHEET] Timesheet {} rejected", event.getReferenceId());
        } catch (Exception e) {
            log.error("[TIMESHEET] Failed to reject timesheet {}: {}", event.getReferenceId(), e.getMessage());
            throw e;
        }
    }
}
