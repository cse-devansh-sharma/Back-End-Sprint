package com.cap.admin.messaging.dto;

import lombok.*;

/**
 * Published by admin-service when a manager approves/rejects.
 * Consumed by timesheet-service and leave-service to update status.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApproveCommandEvent {
    private Long   referenceId;   // timesheetId or leaveRequestId
    private Long   approverId;    // manager's userId
    private String remark;
    private String action;        // "APPROVE" or "REJECT"
}
