package com.cap.admin.service;

import com.cap.admin.client.LeaveServiceClient;
import com.cap.admin.client.TimesheetServiceClient;
import com.cap.admin.dto.ApprovalQueueResponseDTO;
import com.cap.admin.dto.LeaveDetailsDTO;
import com.cap.admin.dto.TimesheetDetailsDTO;
import com.cap.admin.entity.ApprovalQueue;
import com.cap.admin.enums.ApprovalStatus;
import com.cap.admin.enums.ReferenceType;
import com.cap.admin.exception.BusinessRuleException;
import com.cap.admin.messaging.dto.ApproveCommandEvent;
import com.cap.admin.messaging.dto.NotificationEvent;
import com.cap.admin.repository.ApprovalQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalQueueRepository approvalQueueRepository;
    @Mock
    private TimesheetServiceClient timesheetServiceClient;
    @Mock
    private LeaveServiceClient leaveServiceClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ApprovalService approvalService;

    private ApprovalQueue approvalItem;
    private Long managerId = 1L;

    @BeforeEach
    void setUp() {
        approvalItem = ApprovalQueue.builder()
                .id(1L)
                .referenceId(100L)
                .referenceType(ReferenceType.LEAVE)
                .requestedBy(2L)
                .assignedTo(managerId)
                .status(ApprovalStatus.PENDING)
                .build();
    }

    @Test
    void getPendingApprovals_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApprovalQueue> page = new PageImpl<>(Collections.singletonList(approvalItem));

        LeaveDetailsDTO leaveDetails = new LeaveDetailsDTO();
        // optionally set some fields if needed for assertions

        when(approvalQueueRepository.findByAssignedToAndStatus(anyLong(), any(), any())).thenReturn(page);
        when(leaveServiceClient.getLeaveRequestById(100L)).thenReturn(leaveDetails);

        Page<ApprovalQueueResponseDTO> response = approvalService.getPendingApprovals(managerId, "EMPLOYEE", pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(leaveDetails, response.getContent().get(0).getDetails());
    }

    @Test
    void getPendingApprovals_Timesheet_Success() {
        approvalItem.setReferenceType(ReferenceType.TIMESHEET);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApprovalQueue> page = new PageImpl<>(Collections.singletonList(approvalItem));

        TimesheetDetailsDTO tsDetails = new TimesheetDetailsDTO();

        when(approvalQueueRepository.findByAssignedToAndStatus(anyLong(), any(), any())).thenReturn(page);
        when(timesheetServiceClient.getTimesheetById(100L)).thenReturn(tsDetails);

        Page<ApprovalQueueResponseDTO> response = approvalService.getPendingApprovals(managerId, "EMPLOYEE", pageable);

        assertNotNull(response);
        assertEquals(tsDetails, response.getContent().get(0).getDetails());
    }

    @Test
    void approveItem_Success() {
        when(approvalQueueRepository.findById(1L)).thenReturn(Optional.of(approvalItem));

        String response = approvalService.approveItem(1L, "Looks good");

        assertEquals("Item approved successfully", response);
        assertEquals(ApprovalStatus.APPROVED, approvalItem.getStatus());
        verify(approvalQueueRepository, times(1)).save(approvalItem);
        verify(rabbitTemplate, atLeast(1)).convertAndSend(anyString(), anyString(), any(ApproveCommandEvent.class));
        verify(rabbitTemplate, atLeast(1)).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    void approveItem_AlreadyActioned_ThrowsException() {
        approvalItem.setStatus(ApprovalStatus.APPROVED);
        when(approvalQueueRepository.findById(1L)).thenReturn(Optional.of(approvalItem));

        assertThrows(BusinessRuleException.class, () -> approvalService.approveItem(1L, "Retry"));
    }

    @Test
    void rejectItem_Success() {
        when(approvalQueueRepository.findById(1L)).thenReturn(Optional.of(approvalItem));

        String response = approvalService.rejectItem(1L, "Denied");

        assertEquals("Item rejected successfully", response);
        assertEquals(ApprovalStatus.REJECTED, approvalItem.getStatus());
        verify(approvalQueueRepository, times(1)).save(approvalItem);
        verify(rabbitTemplate, atLeast(1)).convertAndSend(anyString(), anyString(), any(ApproveCommandEvent.class));
    }

    @Test
    void rejectItem_RemarkMissing_ThrowsException() {
        assertThrows(BusinessRuleException.class, () -> approvalService.rejectItem(1L, ""));
    }

    @Test
    void addToQueue_NewItem_Success() {
        when(approvalQueueRepository.existsByReferenceIdAndReferenceTypeAndStatus(anyLong(), any(), any())).thenReturn(false);

        approvalService.addToQueue(100L, ReferenceType.LEAVE, 2L, 1L);

        verify(approvalQueueRepository, times(1)).save(any(ApprovalQueue.class));
    }

    @Test
    void addToQueue_DuplicateItem_DoesNotSave() {
        when(approvalQueueRepository.existsByReferenceIdAndReferenceTypeAndStatus(anyLong(), any(), any())).thenReturn(true);

        approvalService.addToQueue(100L, ReferenceType.LEAVE, 2L, 1L);

        verify(approvalQueueRepository, never()).save(any());
    }
}
