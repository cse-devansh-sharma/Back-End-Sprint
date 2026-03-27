package com.cap.leave.messaging.dto;

import lombok.*;

/**
 * Consumed by leave-service from admin.commands exchange.
 * Triggers approveLeave() or rejectLeave() in LeaveService.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApproveCommandEvent {
    private Long   referenceId;  // leaveRequestId
    private Long   approverId;
    private String remark;
    private String action;       // "APPROVE" or "REJECT"
}
