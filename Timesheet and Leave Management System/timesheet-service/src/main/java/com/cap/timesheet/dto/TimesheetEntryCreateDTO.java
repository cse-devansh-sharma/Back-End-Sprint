package com.cap.timesheet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TimesheetEntryCreateDTO {

    @NotNull(message = "Project is required")
    private Long projectId;

    @NotNull(message = "Work date is required")
    private LocalDate workDate;

    @Positive(message = "Hours must be greater than 0")
    private double hoursLogged;

    private String taskSummary;
}