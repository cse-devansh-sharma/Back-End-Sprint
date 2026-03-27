package com.cap.timesheet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimesheetEntryResponseDTO {

    private Long       id;
    private Long       projectId;
    private String     projectName;
    private LocalDate  workDate;
    private BigDecimal hoursLogged;
    private String     taskSummary;
}