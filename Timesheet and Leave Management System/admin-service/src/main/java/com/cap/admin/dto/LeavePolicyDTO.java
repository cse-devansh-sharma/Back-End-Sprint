package com.cap.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeavePolicyDTO {

    private Long       id;
    private String     policyCode;
    private String     leaveTypeCode;
    private Integer    year;
    private BigDecimal allotmentDays;
    private BigDecimal carryForwardDays;
    private Boolean    encashmentAllowed;
    private Boolean    probationExclusion;
    private Boolean    isActive;
}