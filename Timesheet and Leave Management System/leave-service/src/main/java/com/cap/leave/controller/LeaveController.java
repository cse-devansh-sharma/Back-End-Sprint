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

    // ── GET /leave/requests/{id} ─────────────────────────
    // Get single leave request details
    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<LeaveRequestResponseDTO> getById(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(
                leaveService.getLeaveRequestById(id, userId));
    }

    // ── GET /leave/history ───────────────────────────────
    // Paginated leave history for logged in employee
    // ?page=0&size=10&sort=submittedAt,desc
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
    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<String> cancelLeave(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(
                leaveService.cancelLeave(id, userId));
    }

    // ── GET /leave/balance/{userId} ──────────────────────
    // Get leave balances by type for a user
    @GetMapping("/balance/{userId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<List<LeaveBalanceDTO>> getBalances(
            @PathVariable Long userId,
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
    // Internal API used by TimesheetService to check if user is on leave
    @GetMapping("/users/{userId}/on-leave")
    public ResponseEntity<Boolean> isOnLeave(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(leaveService.isOnLeave(userId, date));
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