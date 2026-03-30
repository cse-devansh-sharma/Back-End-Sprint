package com.cap.timesheet.client;

import com.cap.timesheet.dto.OnLeaveStatusResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.*;
import java.time.LocalDate;

@FeignClient(name = "leave-service")
public interface LeaveServiceClient {

    @GetMapping("/leave/internal/users/{userId}/on-leave")
    OnLeaveStatusResponseDTO isOnLeave( @PathVariable("userId") Long userId, @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date);

    @GetMapping("/leave/holidays")
    List<com.cap.timesheet.dto.HolidayDTO> getHolidays(@RequestParam(value = "year", required = false) Integer year);
}
