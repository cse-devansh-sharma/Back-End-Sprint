package com.cap.admin.entity;

import com.cap.admin.enums.ApprovalStatus;
import com.cap.admin.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalQueue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID from timesheet_db or leave_db
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private ReferenceType referenceType; // TIMESHEET or LEAVE

    // employee who submitted
    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    // manager assigned to review
    @Column(name = "assigned_to", nullable = false)
    private Long assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;
}