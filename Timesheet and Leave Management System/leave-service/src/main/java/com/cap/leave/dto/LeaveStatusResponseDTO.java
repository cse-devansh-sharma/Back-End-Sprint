package com.cap.leave.dto;

import com.cap.leave.enums.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveStatusResponseDTO {
    private Long leaveId;
    private LeaveStatus status;
}
