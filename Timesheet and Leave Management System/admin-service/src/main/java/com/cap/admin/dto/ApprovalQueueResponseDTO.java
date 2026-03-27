package com.cap.admin.dto;

import com.cap.admin.enums.ApprovalStatus;
import com.cap.admin.enums.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalQueueResponseDTO {

    private Long           id;
    private Long           referenceId;
    private ReferenceType  referenceType;
    private Long           requestedBy;
    private String         requesterName;  // enriched from auth
    private ApprovalStatus status;
    private String         remark;
    private LocalDateTime  createdAt;
    private LocalDateTime  actionedAt;

    // enriched details from Timesheet or Leave service
    private Object         details;
}