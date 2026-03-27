package com.cap.admin.messaging.dto;

import lombok.*;

/**
 * Published by timesheet-service when a week is submitted.
 * Consumed by admin-service to populate the approval queue.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimesheetSubmittedEvent {
    private Long   timesheetId;
    private Long   userId;       // employee who submitted
    private Long   managerId;    // manager to assign for approval
    private String weekStart;    // ISO date string e.g. "2026-03-23"
    private Double totalHours;
}
