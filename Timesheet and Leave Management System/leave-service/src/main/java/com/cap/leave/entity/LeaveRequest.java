package com.cap.leave.entity;

import com.cap.leave.enums.LeaveStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId from JWT — no FK to auth_db
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // FK within same schema
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    // calculated: excludes weekends and holidays
    @Column(name = "number_of_days", nullable = false,
            precision = 4, scale = 1)
    private BigDecimal numberOfDays;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    // nullable — only required if leaveType.requiresDelegate = true
    @Column(name = "delegate_user_id")
    private Long delegateUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.DRAFT;

    // manager who reviewed this request
    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "manager_remark", columnDefinition = "TEXT")
    private String managerRemark;

    @Column(name = "hr_remark", columnDefinition = "TEXT")
    private String hrRemark;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;
}