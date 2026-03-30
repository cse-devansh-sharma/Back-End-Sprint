package com.cap.admin.service;

import com.cap.admin.client.LeaveServiceClient;
import com.cap.admin.client.TimesheetServiceClient;
import com.cap.admin.dto.DashboardComplianceDTO;
import com.cap.admin.dto.DashboardEmployeeSummaryDTO;
import com.cap.admin.enums.ApprovalStatus;
import com.cap.admin.repository.ApprovalQueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ApprovalQueueRepository approvalQueueRepository;
    @Mock
    private TimesheetServiceClient timesheetServiceClient;
    @Mock
    private LeaveServiceClient leaveServiceClient;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getComplianceSummary_Success() {
        Long managerId = 1L;
        when(approvalQueueRepository.countByAssignedToAndStatus(managerId, ApprovalStatus.PENDING)).thenReturn(5L);
        when(approvalQueueRepository.countByAssignedToAndStatus(managerId, ApprovalStatus.APPROVED)).thenReturn(10L);
        when(approvalQueueRepository.countByAssignedToAndStatus(managerId, ApprovalStatus.REJECTED)).thenReturn(5L);

        DashboardComplianceDTO response = dashboardService.getComplianceSummary(managerId, "EMPLOYEE");

        assertNotNull(response);
        assertEquals(20, response.getTotalSubmitted());
        assertEquals(10, response.getTotalApproved());
        assertEquals(5, response.getTotalPending());
        assertEquals(50.0, response.getCompliancePercent(), 0.001);
    }

    @Test
    void getComplianceSummary_ZeroTotal_Returns100() {
        Long managerId = 1L;
        when(approvalQueueRepository.countByAssignedToAndStatus(anyLong(), any())).thenReturn(0L);

        DashboardComplianceDTO response = dashboardService.getComplianceSummary(managerId, "EMPLOYEE");

        assertEquals(100.0, response.getCompliancePercent(), 0.001);
    }

    @Test
    void getEmployeeSummary_Success() {
        Long userId = 1L;
        when(timesheetServiceClient.getHistory(anyLong(), anyInt(), anyInt())).thenReturn("tsData");
        when(leaveServiceClient.getBalances(anyLong())).thenReturn("leaveData");

        DashboardEmployeeSummaryDTO response = dashboardService.getEmployeeSummary(userId);

        assertNotNull(response);
        assertEquals("tsData", response.getLastTimesheetData());
        assertEquals("leaveData", response.getLeaveBalanceData());
    }

    @Test
    void getEmployeeSummary_ServiceFailure_GracefulDegradation() {
        Long userId = 1L;
        when(timesheetServiceClient.getHistory(anyLong(), anyInt(), anyInt())).thenThrow(new RuntimeException("Service down"));
        when(leaveServiceClient.getBalances(anyLong())).thenReturn("leaveData");

        DashboardEmployeeSummaryDTO response = dashboardService.getEmployeeSummary(userId);

        assertNotNull(response);
        assertNull(response.getLastTimesheetData());
        assertEquals("leaveData", response.getLeaveBalanceData());
    }
}
