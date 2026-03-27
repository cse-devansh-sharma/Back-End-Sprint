package com.cap.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaveBalanceDTO {

    private String     leaveTypeCode;
    private String     leaveTypeName;
    private BigDecimal totalAllotted;
    private BigDecimal used;
    private BigDecimal pending;
    // calculated: totalAllotted - used - pending
    private BigDecimal remaining;
}