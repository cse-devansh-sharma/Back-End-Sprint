package com.cap.leave.messaging.dto;

import lombok.*;

/**
 * Published by leave-service to leave.events exchange when a request is submitted.
 * Consumed by admin-service to auto-populate approval_queue.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveSubmittedEvent {
    private Long   leaveRequestId;
    private Long   userId;
    private Long   managerId;
    private String fromDate;
    private String toDate;
    private String leaveTypeName;
    private Double numberOfDays;
}
