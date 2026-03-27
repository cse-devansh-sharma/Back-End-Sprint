package com.cap.admin.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TimesheetDetailsDTO {

    private Long       id;
    private LocalDate  weekStart;
    private BigDecimal totalHours;
    private String     status;
    private String     employeeComment;
}