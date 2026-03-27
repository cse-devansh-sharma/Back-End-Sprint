package com.cap.timesheet.controller;

import io.swagger.v3.oas.annotations.Operation;

import com.cap.timesheet.dto.*;
import com.cap.timesheet.service.TimesheetService;

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
@RequestMapping("/timesheet")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")  // ← add this
public class TimesheetController {

    private final TimesheetService timesheetService;

    // ── POST /timesheet/entries ──────────────────────────
    // Employee saves a daily entry
    @Operation(summary = "Save Timesheet Entry", description = "Allows an employee to create or update a time log for a specific project on a valid work day.")
    @PostMapping("/entries")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TimesheetEntryResponseDTO> saveEntry( @Valid @RequestBody TimesheetEntryCreateDTO request, Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.status(201).body(timesheetService.saveEntry(request, userId));
    }

    // ── GET /timesheet/weeks/{weekStart} ─────────────────
    // Fetch weekly timesheet with all entries
    // weekStart must be a Monday e.g. 2026-03-23
    @Operation(summary = "Get Weekly Timesheet", description = "Retrieves a draft or submitted timesheet along with all of its logged hours for a specific week starting on Monday.")
    @GetMapping("/weeks/{weekStart}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<WeeklyTimesheetDTO> getWeeklyTimesheet(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart, Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(timesheetService.getWeeklyTimesheet(weekStart, userId));
    }

    // ── DELETE /timesheet/entries ───────────────────
    // Delete a draft entry — blocked if week is SUBMITTED+
    @Operation(summary = "Delete Timesheet Entry", description = "Deletes a draft timesheet entry matching the provided projectId, using either an exact entryId or a specific date.")
    @DeleteMapping("/entries")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> deleteEntry(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long entryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(timesheetService.deleteEntry(projectId, entryId, date, userId));
    }

    // ── GET /timesheet/weeks/{weekStart}/validate ────────
    // Pre-submit validation — shows missing days + violations
    @Operation(summary = "Validate Weekly Timesheet", description = "Simulates rules check mapping against a week ensuring 40hr metrics and holiday/leave compliances.")
    @GetMapping("/weeks/{weekStart}/validate")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ValidationResultDTO> validateWeek(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart, Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(timesheetService.validateWeek(weekStart, userId));
    }

    // ── POST /timesheet/weeks/{weekStart}/submit ─────────
    // Submit week for manager approval
    @Operation(summary = "Submit Week", description = "Validates and submits the final draft of a timesheet block to an approval queue.")
    @PostMapping("/weeks/{weekStart}/submit")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> submitWeek(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart, @RequestBody(required = false) SubmitWeekDTO submitRequest, Authentication authentication) {

        Long userId = extractUserId(authentication);
        String comment = submitRequest != null ? submitRequest.getComment() : null;
        
        return ResponseEntity.ok(timesheetService.submitWeek(weekStart, comment, userId));
    }

    // ── GET /timesheet/projects ──────────────────────────
    // List active projects from read-model
    @Operation(summary = "List Active Projects", description = "Fetches a localized list of standard operating projects accessible by staff.")
    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ProjectDTO>> getProjects() {
        return ResponseEntity.ok(timesheetService.getActiveProjects());
    }

    // ── GET /timesheet/history ───────────────────────────
    // Paginated timesheet history for employee
    // ?page=0&size=10
    @Operation(summary = "Get User Timesheet History", description = "Retrieves all timesheets logged by the user, dynamically filtered by an optional projectId.")
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Page<WeeklyTimesheetDTO>> getHistory(
            @RequestParam(required = false) Long projectId,
            Authentication authentication, 
            Pageable pageable) {
    	
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(timesheetService.getHistory(userId, pageable, projectId));
    }

    // ── private helper ───────────────────────────────────
    private Long extractUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}