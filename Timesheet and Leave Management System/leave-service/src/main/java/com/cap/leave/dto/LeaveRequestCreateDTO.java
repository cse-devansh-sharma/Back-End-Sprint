package com.cap.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestCreateDTO {

    @NotNull(message = "Leave type is required")
    private Long leaveTypeId;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    @NotBlank(message = "Reason is required")
    private String reason;

    // only required if leave type needs delegate
    // validated at service layer
    private Long delegateUserId;
}