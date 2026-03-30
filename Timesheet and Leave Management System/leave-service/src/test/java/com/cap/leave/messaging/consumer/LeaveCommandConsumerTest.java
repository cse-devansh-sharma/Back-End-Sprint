package com.cap.leave.messaging.consumer;

import com.cap.leave.messaging.dto.ApproveCommandEvent;
import com.cap.leave.service.LeaveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveCommandConsumerTest {

    @Mock
    private LeaveService leaveService;

    @InjectMocks
    private LeaveCommandConsumer leaveCommandConsumer;

    @Test
    void onLeaveApproveCommand_Success() {
        ApproveCommandEvent event = ApproveCommandEvent.builder()
                .referenceId(1L)
                .approverId(2L)
                .remark("Approved via RabbitMQ")
                .action("APPROVE")
                .build();

        leaveCommandConsumer.onApproveCommand(event);

        verify(leaveService, times(1)).approveLeave(1L, 2L, "Approved via RabbitMQ");
    }

    @Test
    void onLeaveRejectCommand_Success() {
        ApproveCommandEvent event = ApproveCommandEvent.builder()
                .referenceId(1L)
                .approverId(2L)
                .remark("Rejected via RabbitMQ")
                .action("REJECT")
                .build();

        leaveCommandConsumer.onRejectCommand(event);

        verify(leaveService, times(1)).rejectLeave(1L, 2L, "Rejected via RabbitMQ");
    }
}
