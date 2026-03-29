package com.cap.admin.client;

import com.cap.admin.dto.TimesheetDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "timesheet-service")
public interface TimesheetServiceClient {

    @GetMapping("/timesheet/weeks/id/{id}")
    TimesheetDetailsDTO getTimesheetById(@PathVariable("id") Long id);

    @GetMapping("/timesheet/history")
    Object getHistory(@RequestParam("userId") Long userId, @RequestParam("page") int page, @RequestParam("size") int size);
}
