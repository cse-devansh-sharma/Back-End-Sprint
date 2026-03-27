package com.cap.admin.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaveDetailsDTO {

    private Long       id;
    private String     leaveTypeName;
    private LocalDate  fromDate;
    private LocalDate  toDate;
    private BigDecimal numberOfDays;
    private String     status;
    private String     reason;
}