package com.cap.admin.service;

import com.cap.admin.dto.*;
import com.cap.admin.enums.ApprovalStatus;
import com.cap.admin.repository.ApprovalQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.cap.admin.client.LeaveServiceClient;
import com.cap.admin.client.TimesheetServiceClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ApprovalQueueRepository approvalQueueRepository;
    private final TimesheetServiceClient timesheetServiceClient;
    private final LeaveServiceClient leaveServiceClient;

    /**
     * Compliance summary: shows what % of submitted timesheets were approved
     * for a manager's team or the entire organization if ADMIN/MANAGER.
     */
    public DashboardComplianceDTO getComplianceSummary(Long managerId, String role) {

        long total, approved, pending;

        if ("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
            // global stats for privileged users
            total = approvalQueueRepository.countByStatus(ApprovalStatus.PENDING)
                    + approvalQueueRepository.countByStatus(ApprovalStatus.APPROVED)
                    + approvalQueueRepository.countByStatus(ApprovalStatus.REJECTED);
            approved = approvalQueueRepository.countByStatus(ApprovalStatus.APPROVED);
            pending = approvalQueueRepository.countByStatus(ApprovalStatus.PENDING);
        } else {
            // manager specific stats
            total = approvalQueueRepository.countByAssignedToAndStatus(managerId, ApprovalStatus.PENDING)
                    + approvalQueueRepository.countByAssignedToAndStatus(managerId, ApprovalStatus.APPROVED)
                    + approvalQueueRepository.countByAssignedToAndStatus(managerId, ApprovalStatus.REJECTED);
            approved = approvalQueueRepository.countByAssignedToAndStatus(managerId, ApprovalStatus.APPROVED);
            pending = approvalQueueRepository.countByAssignedToAndStatus(managerId, ApprovalStatus.PENDING);
        }

        double compliancePct = total == 0 ? 100.0
                : Math.round((approved * 100.0 / total) * 10.0) / 10.0;

        return DashboardComplianceDTO.builder()
                .managerId(managerId)
                .totalSubmitted(total)
                .totalApproved(approved)
                .totalPending(pending)
                .compliancePercent(compliancePct)
                .build();
    }

    /**
     * Employee summary: fetches last timesheet status from timesheet-service
     * and leave balance from leave-service.
     */
    public DashboardEmployeeSummaryDTO getEmployeeSummary(Long userId) {
        DashboardEmployeeSummaryDTO summary = DashboardEmployeeSummaryDTO.builder()
                .userId(userId)
                .build();

        try {
            Object tsHistory = timesheetServiceClient.getHistory(userId, 0, 1);
            summary.setLastTimesheetData(tsHistory);
        } catch (Exception e) {
            log.warn("[ADMIN] Could not fetch timesheet summary for user {}: {}", userId, e.getMessage());
        }

        try {
            Object leaveBalances = leaveServiceClient.getBalances(userId);
            summary.setLeaveBalanceData(leaveBalances);
        } catch (Exception e) {
            log.warn("[ADMIN] Could not fetch leave balance for user {}: {}", userId, e.getMessage());
        }

        return summary;
    }
}
