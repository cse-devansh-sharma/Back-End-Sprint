package com.cap.timesheet.messaging.dto;

import lombok.*;

/**
 * Published by timesheet-service when submitWeek() succeeds.
 * Consumed by admin-service to populate approval queue.
 * Consumed by notification-service to notify the employee.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimesheetSubmittedEvent {
    private Long   timesheetId;
    private Long   userId;      // employee who submitted
    private Long   managerId;   // assigned reviewer
    private String weekStart;   // ISO date e.g. "2026-03-23"
    private Double totalHours;
}
