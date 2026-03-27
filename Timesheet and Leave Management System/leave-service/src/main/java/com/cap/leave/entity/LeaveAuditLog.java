package com.cap.leave.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which leave request this log belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private LeaveRequest leaveRequest;

    // SUBMITTED, APPROVED, REJECTED, CANCELLED, AVAILED
    @Column(nullable = false, length = 60)
    private String action;

    // userId of who performed the action
    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}