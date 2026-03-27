package com.cap.timesheet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "timesheet_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timesheet_id", nullable = false)
    private Long timesheetId;

    @Column(nullable = false)
    private String action;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "performed_at")
    private LocalDateTime performedAt;

    private String remarks;
}