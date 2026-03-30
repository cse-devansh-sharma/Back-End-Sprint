package com.cap.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveAllocationRequestDTO {
    private Long userId;
    private String leaveTypeCode;
    private BigDecimal amount;
    private Integer year;
}
