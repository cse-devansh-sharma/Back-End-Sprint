package com.cap.timesheet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "timesheet_entries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id", "work_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "weekly_timesheet_id", nullable = false)
    private WeeklyTimesheet weeklyTimesheet;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "hours_logged", nullable = false)
    private BigDecimal hoursLogged;

    @Column(name = "task_summary")
    private String taskSummary;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;
}