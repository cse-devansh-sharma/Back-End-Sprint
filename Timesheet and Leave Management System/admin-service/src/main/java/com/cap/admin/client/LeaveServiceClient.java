package com.cap.admin.client;

import com.cap.admin.dto.LeaveDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "leave-service")
public interface LeaveServiceClient {

    @GetMapping("/leave/requests/{leaveId}")
    LeaveDetailsDTO getLeaveRequestById(@PathVariable("leaveId") Long leaveId);

    @GetMapping("/leave/balance/{userId}")
    Object getBalances(@PathVariable("userId") Long userId);
}
