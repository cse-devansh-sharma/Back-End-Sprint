package com.cap.admin.controller;

import com.cap.admin.dto.*;
import com.cap.admin.service.MasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/master")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class MasterDataController {

    private final MasterDataService masterDataService;

    // ── PROJECTS ─────────────────────────────────────────

    @Operation(summary = "List All Projects",
            description = "Returns all projects (active and inactive). Restricted to ADMIN and HR roles.")
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDTO>> getProjects() {
        return ResponseEntity.ok(masterDataService.getAllProjects());
    }

    @Operation(summary = "Create Project",
            description = "Creates a new project with a unique project code and publishes a ProjectCreated event to the master.data.sync fanout exchange.")
    @PostMapping("/projects")
    public ResponseEntity<ProjectDTO> createProject(
            @RequestBody ProjectDTO dto,
            Authentication authentication) {
        Long createdBy = Long.parseLong(authentication.getName());
        return ResponseEntity.status(201)
                .body(masterDataService.createProject(dto, createdBy));
    }

    @Operation(summary = "Update Project",
            description = "Updates an existing project and publishes a ProjectUpdated event to sync timesheet local read-model.")
    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectDTO dto) {
        return ResponseEntity.ok(masterDataService.updateProject(id, dto));
    }

    @Operation(summary = "Deactivate Project",
            description = "Soft-deletes a project (sets isActive=false) and publishes a ProjectDeactivated event.")
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<ProjectDTO> deactivateProject(@PathVariable Long id) {
        return ResponseEntity.ok(masterDataService.deactivateProject(id));
    }

    // ── COST CENTERS ─────────────────────────────────────

    @Operation(summary = "List Cost Centers",
            description = "Returns all cost centers.")
    @GetMapping("/cost-centers")
    public ResponseEntity<List<CostCenterDTO>> getCostCenters() {
        return ResponseEntity.ok(masterDataService.getAllCostCenters());
    }

    @Operation(summary = "Create Cost Center",
            description = "Creates a new cost center with a unique code.")
    @PostMapping("/cost-centers")
    public ResponseEntity<CostCenterDTO> createCostCenter(@RequestBody CostCenterDTO dto) {
        return ResponseEntity.status(201)
                .body(masterDataService.createCostCenter(dto));
    }

    @Operation(summary = "Update Cost Center",
            description = "Updates an existing cost center name or status.")
    @PutMapping("/cost-centers/{id}")
    public ResponseEntity<CostCenterDTO> updateCostCenter(
            @PathVariable Long id,
            @RequestBody CostCenterDTO dto) {
        return ResponseEntity.ok(masterDataService.updateCostCenter(id, dto));
    }

    // ── LEAVE POLICIES ────────────────────────────────────

    @Operation(summary = "List Leave Policies",
            description = "Returns all leave policies across all years.")
    @GetMapping("/leave-policies")
    public ResponseEntity<List<LeavePolicyDTO>> getLeavePolicies() {
        return ResponseEntity.ok(masterDataService.getAllLeavePolicies());
    }

    @Operation(summary = "Create Leave Policy",
            description = "Creates a new leave policy for a leave type + year combination. Unique constraint enforced.")
    @PostMapping("/leave-policies")
    public ResponseEntity<LeavePolicyDTO> createLeavePolicy(@RequestBody LeavePolicyDTO dto) {
        return ResponseEntity.status(201)
                .body(masterDataService.createLeavePolicy(dto));
    }

    @Operation(summary = "Update Leave Policy",
            description = "Updates allotment days, carry-forward, and active status for an existing leave policy.")
    @PutMapping("/leave-policies/{id}")
    public ResponseEntity<LeavePolicyDTO> updateLeavePolicy(
            @PathVariable Long id,
            @RequestBody LeavePolicyDTO dto) {
        return ResponseEntity.ok(masterDataService.updateLeavePolicy(id, dto));
    }
}
