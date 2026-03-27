package com.cap.timesheet.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HolidayDTO {

    private LocalDate holidayDate;
    private String    name;
    private String    type;
}