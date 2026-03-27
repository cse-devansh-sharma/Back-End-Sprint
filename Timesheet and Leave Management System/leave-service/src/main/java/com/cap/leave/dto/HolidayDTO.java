package com.cap.leave.dto;

import com.cap.leave.enums.HolidayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HolidayDTO {

    private Long        id;
    private LocalDate   holidayDate;
    private String      name;
    private HolidayType type;
    private Integer     year;
}