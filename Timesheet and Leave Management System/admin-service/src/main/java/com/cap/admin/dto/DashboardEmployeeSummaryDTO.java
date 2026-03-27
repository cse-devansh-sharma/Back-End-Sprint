package com.cap.admin.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardEmployeeSummaryDTO {
    private Long   userId;
    private Object lastTimesheetData;
    private Object leaveBalanceData;
}
