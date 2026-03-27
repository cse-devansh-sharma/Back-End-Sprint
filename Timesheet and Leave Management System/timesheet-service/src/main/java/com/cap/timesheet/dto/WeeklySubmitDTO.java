package com.cap.timesheet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WeeklySubmitDTO {

    @NotNull
    private LocalDate weekStart;

    private String comment;
}