package com.cap.leave.controller;

import com.cap.leave.dto.*;
import com.cap.leave.service.LeaveService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // ── POST /leave/requests ─────────────────────────────
    // Employee applies for leave
    @io.swagger.v3.oas.annotations.Operation(summary = "Create a new leave request")
    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<LeaveRequestResponseDTO> createLeaveRequest(
            @Valid @RequestBody LeaveRequestCreateDTO request,
            Authentication authentication) {

        // extract userId from JWT via SecurityContext
        // never trust userId from request body
        Long userId = extractUserId(authentication);
        return ResponseEntity.status(201)
                .body(leaveService.createLeaveRequest(request, userId));
    }

    // ── GET /leave/requests/{leaveId}/status ─────────────────────────
    // Get single leave request status
    @io.swagger.v3.oas.annotations.Operation(summary = "Check leave request status", description = "Enter the Leave request ID to check the status (like approved, pending, etc.)")
    @GetMapping("/requests/{leaveId}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<LeaveStatusResponseDTO> getLeaveStatusById(
            @io.swagger.v3.oas.annotations.Parameter(description = "Leave Request ID") @PathVariable("leaveId") Long leaveId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(
                leaveService.getLeaveStatusById(leaveId, userId));
    }

    // ── GET /leave/history ───────────────────────────────
    // Paginated leave history for logged in employee
    // ?page=0&size=10&sort=submittedAt,desc
    @io.swagger.v3.oas.annotations.Operation(summary = "Get leave history", description = "Retrieve a paginated history of leave requests for the logged-in employee.")
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<Page<LeaveRequestResponseDTO>> getLeaveHistory(
            Authentication authentication,
            Pageable pageable) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(
                leaveService.getLeaveHistory(userId, pageable));
    }

    // ── DELETE /leave/requests/{id} ──────────────────────
    // Employee cancels their leave request
    @io.swagger.v3.oas.annotations.Operation(summary = "Cancel a leave request")
    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<String> cancelLeave(
            @io.swagger.v3.oas.annotations.Parameter(description = "Leave Request ID") @PathVariable Long id,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(
                leaveService.cancelLeave(id, userId));
    }

    // ── GET /leave/balance/{userId} ──────────────────────
    // Get leave balances by type for a user
    @io.swagger.v3.oas.annotations.Operation(summary = "Get leave balance", description = "Get leave balances by leave type for a specific user.")
    @GetMapping("/balance/{userId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<List<LeaveBalanceDTO>> getBalances(
            @io.swagger.v3.oas.annotations.Parameter(description = "User ID") @PathVariable Long userId,
            Authentication authentication) {

        // employee can only view own balance
        // manager/admin can view anyone's
        String role = extractRole(authentication);
        Long currentUserId = extractUserId(authentication);

        if (role.equals("ROLE_EMPLOYEE")
                && !currentUserId.equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only view your own balance");
        }

        return ResponseEntity.ok(
                leaveService.getLeaveBalances(userId));
    }

    // ── GET /leave/team-calendar ─────────────────────────
    // Manager views team leave calendar for a month
    // ?monthStart=2025-04-01&monthEnd=2025-04-30
    @io.swagger.v3.oas.annotations.Operation(summary = "Get team leave calendar")
    @GetMapping("/team-calendar")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<List<TeamCalendarDTO>> getTeamCalendar(
            @RequestParam List<Long> userIds,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate monthStart,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate monthEnd) {

        return ResponseEntity.ok(
                leaveService.getTeamCalendar(
                        userIds, monthStart, monthEnd));
    }

    // ── GET /leave/holidays ──────────────────────────────
    // Get holiday list for a year
    // ?year=2025
    @io.swagger.v3.oas.annotations.Operation(summary = "Get holidays list")
    @GetMapping("/holidays")
    public ResponseEntity<List<HolidayDTO>> getHolidays(
            @RequestParam(required = false)
            Integer year) {

        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return ResponseEntity.ok(
                leaveService.getHolidaysByYear(year));
    }

    // ── GET /leave/users/{userId}/on-leave ───────────────
    // API restricted to Authorities only
    @io.swagger.v3.oas.annotations.Operation(summary = "Check if user is on leave (Authorities only)", description = "Returns onLeave status. Restricted to MANAGERS, HR, and ADMIN.")
    @GetMapping("/users/{userId}/on-leave")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<OnLeaveStatusResponseDTO> isOnLeave(
            @io.swagger.v3.oas.annotations.Parameter(description = "User ID") @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        boolean isOnLeaveStatus = leaveService.isOnLeave(userId, date);
        return ResponseEntity.ok(new OnLeaveStatusResponseDTO(isOnLeaveStatus));
    }

    // ── GET /leave/internal/users/{userId}/on-leave ──────
    // Internal API used by TimesheetService (PermitAll in SecurityConfig)
    @io.swagger.v3.oas.annotations.Operation(summary = "Internal: Check if user is on leave", description = "Internal fallback for service-to-service validation.")
    @GetMapping("/internal/users/{userId}/on-leave")
    public ResponseEntity<OnLeaveStatusResponseDTO> isOnLeaveInternal(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        boolean isOnLeaveStatus = leaveService.isOnLeave(userId, date);
        return ResponseEntity.ok(new OnLeaveStatusResponseDTO(isOnLeaveStatus));
    }

    // ── GET /leave/requests/{leaveId} ────────────────────
    // Fetch individual leave request details - used by Admin Service
    @io.swagger.v3.oas.annotations.Operation(summary = "Get Leave Request by ID", description = "Internal/Admin lookup for a specific leave request by its unique ID.")
    @GetMapping("/requests/{leaveId}")
    public ResponseEntity<LeaveRequestResponseDTO> getLeaveRequestById(@PathVariable Long leaveId, Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(leaveService.getLeaveRequestById(leaveId, userId));
    }

    // ── GET /leave/types ──────────────────────────────────
    // Returns active leave types with id, code, name for the form dropdown
    @io.swagger.v3.oas.annotations.Operation(summary = "Get active leave types")
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<List<java.util.Map<String, Object>>> getLeaveTypes() {
        return ResponseEntity.ok(
            leaveService.getActiveLeaveTypes()
        );
    }

    // ── POST /leave/internal/users/{userId}/allocate-initial ───────
    // Internal API called by IdentityService via Feign to allocate default leaves
    @io.swagger.v3.oas.annotations.Operation(summary = "Allocate initial leaves", description = "Internal API to allocate default leaves to a newly registered user.")
    @PostMapping("/internal/users/{userId}/allocate-initial")
    public ResponseEntity<String> allocateInitialLeaves(
            @io.swagger.v3.oas.annotations.Parameter(description = "User ID") @PathVariable Long userId) {

        leaveService.allocateInitialLeaves(userId);
        return ResponseEntity.ok("Initial leaves allocated successfully");
    }

    // ── POST /leave/allocate ─────────────────────────────
    // Manual allocation by Authorities
    @io.swagger.v3.oas.annotations.Operation(summary = "Manually allocate leaves", description = "Authorized roles can increase leave balances manually.")
    @PostMapping("/allocate")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<String> manualAllocate(
            @Valid @RequestBody LeaveAllocationRequestDTO request) {

        leaveService.manualAllocateLeaves(request);
        return ResponseEntity.ok("Leaves allocated successfully");
    }

    // ── private helper — extract userId from JWT ─────────
    private Long extractUserId(Authentication authentication) {
        // authentication.getName() returns userId
        // set in JwtAuthFilter as the principal
        return Long.parseLong(authentication.getName());
    }

    // ── private helper — extract role from JWT ───────────
    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities()
                .iterator().next().getAuthority();
    }
}