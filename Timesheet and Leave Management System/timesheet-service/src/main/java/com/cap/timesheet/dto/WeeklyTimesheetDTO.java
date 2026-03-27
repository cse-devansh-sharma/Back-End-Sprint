package com.cap.timesheet.dto;

import com.cap.timesheet.enums.TimesheetStatus;
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
public class WeeklyTimesheetDTO {

    private Long                            id;
    private LocalDate                       weekStart;
    private BigDecimal                      totalHours;
    private TimesheetStatus                 status;
    private String                          employeeComment;
    private String                          managerRemark;
    private List<TimesheetEntryResponseDTO> entries;
}