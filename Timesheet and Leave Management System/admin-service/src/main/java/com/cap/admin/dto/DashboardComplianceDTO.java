package com.cap.admin.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardComplianceDTO {
    private Long   managerId;
    private long   totalSubmitted;
    private long   totalApproved;
    private long   totalPending;
    private double compliancePercent;
}
