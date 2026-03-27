package com.cap.leave.dto;

import com.cap.leave.enums.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamCalendarDTO {

    // one entry per person per leave block
    private LocalDate   date;
    private Long        userId;
    private String      userName;   // fetched via JWT/header or passed in
    private String      leaveType;  // e.g. Casual Leave
    private LeaveStatus status;
    private LocalDate   fromDate;
    private LocalDate   toDate;
}