package com.cap.timesheet.messaging.consumer;

import com.cap.timesheet.messaging.dto.ApproveCommandEvent;
import com.cap.timesheet.service.TimesheetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetCommandConsumerTest {

    @Mock
    private TimesheetService timesheetService;

    @InjectMocks
    private TimesheetCommandConsumer timesheetCommandConsumer;

    @Test
    void onApproveCommand_Success() {
        ApproveCommandEvent event = ApproveCommandEvent.builder()
                .referenceId(1L)
                .approverId(2L)
                .remark("Approved via RabbitMQ")
                .action("APPROVE")
                .build();

        timesheetCommandConsumer.onApproveCommand(event);

        verify(timesheetService, times(1)).approveTimesheet(1L, 2L, "Approved via RabbitMQ");
    }

    @Test
    void onRejectCommand_Success() {
        ApproveCommandEvent event = ApproveCommandEvent.builder()
                .referenceId(1L)
                .approverId(2L)
                .remark("Rejected via RabbitMQ")
                .action("REJECT")
                .build();

        timesheetCommandConsumer.onRejectCommand(event);

        verify(timesheetService, times(1)).rejectTimesheet(1L, 2L, "Rejected via RabbitMQ");
    }
}
