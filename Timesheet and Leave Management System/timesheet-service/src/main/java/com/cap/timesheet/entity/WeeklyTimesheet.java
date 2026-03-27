package com.cap.timesheet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.cap.timesheet.enums.TimesheetStatus;

@Entity
@Table(name = "weekly_timesheets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyTimesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "total_hours")
    private BigDecimal totalHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimesheetStatus status;

    @Column(name = "employee_comment")
    private String employeeComment;

    @Column(name = "manager_remark")
    private String managerRemark;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "actioned_by")
    private Long actionedBy;

    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    // 🔥 Relation
    @OneToMany(mappedBy = "weeklyTimesheet", cascade = CascadeType.ALL)
    private List<TimesheetEntry> entries;
}