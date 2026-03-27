package com.cap.admin.controller;

import com.cap.admin.dto.*;
import com.cap.admin.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Employee Summary",
            description = "Aggregates the employee's last timesheet status and leave balance by calling timesheet-service and leave-service. Returns partial data if a service is unavailable.")
    @GetMapping("/employee-summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<DashboardEmployeeSummaryDTO> getEmployeeSummary(
            @RequestParam Long userId) {
        return ResponseEntity.ok(dashboardService.getEmployeeSummary(userId));
    }

    @Operation(summary = "Compliance Summary",
            description = "Shows total submitted vs approved requests for the logged-in manager. Calculated purely from the local approval_queue — no external service call needed.")
    @GetMapping("/compliance")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<DashboardComplianceDTO> getComplianceSummary(
            Authentication authentication) {
        Long managerId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(dashboardService.getComplianceSummary(managerId));
    }
}
