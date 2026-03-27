package com.cap.admin.messaging.dto;

import lombok.*;

/**
 * Published by leave-service when a leave request is submitted.
 * Consumed by admin-service to populate the approval queue.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveSubmittedEvent {
    private Long   leaveRequestId;
    private Long   userId;        // employee who applied
    private Long   managerId;     // manager to assign
    private String fromDate;      // ISO date string
    private String toDate;
    private String leaveTypeName;
    private Double numberOfDays;
}
