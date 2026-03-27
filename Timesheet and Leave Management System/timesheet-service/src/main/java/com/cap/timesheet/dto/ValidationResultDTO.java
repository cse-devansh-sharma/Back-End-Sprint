package com.cap.timesheet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResultDTO {

    private boolean          isValid;
    private List<LocalDate>  missingDates;
    private List<String>     violations;
    private BigDecimal       totalHours;
}