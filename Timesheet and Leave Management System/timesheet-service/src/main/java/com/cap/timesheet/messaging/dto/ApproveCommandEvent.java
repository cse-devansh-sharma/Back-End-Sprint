package com.cap.timesheet.messaging.dto;

import lombok.*;

/**
 * Consumed by timesheet-service from admin.commands exchange.
 * Triggers approveTimesheet() or rejectTimesheet() in TimesheetService.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApproveCommandEvent {
    private Long   referenceId;  // timesheetId
    private Long   approverId;
    private String remark;
    private String action;       // "APPROVE" or "REJECT"
}
