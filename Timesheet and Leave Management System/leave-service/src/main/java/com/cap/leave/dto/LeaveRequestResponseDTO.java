package com.cap.leave.dto;

import com.cap.leave.enums.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRequestResponseDTO {

    private Long        id;
    private String      leaveTypeName;
    private String      leaveTypeCode;
    private LocalDate   fromDate;
    private LocalDate   toDate;
    private BigDecimal  numberOfDays;
    private LeaveStatus status;
    private String      reason;
    private String      managerRemark;
    private LocalDateTime submittedAt;
}