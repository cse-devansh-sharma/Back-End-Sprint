package com.cap.admin.controller;

import com.cap.admin.dto.*;
import com.cap.admin.enums.ReferenceType;
import com.cap.admin.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ApprovalController {

    private final ApprovalService approvalService;

    // ── GET /admin/approvals ─────────────────────────────
    // Manager views their pending approval queue
    @Operation(summary = "Get Pending Approvals", description = "Fetches a paginated list of all timesheet and leave requests awaiting approval from the currently logged-in manager.")
    @GetMapping("/approvals")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<ApprovalQueueResponseDTO>> getPendingApprovals( Authentication authentication, Pageable pageable) {

        Long managerId = extractUserId(authentication);
        return ResponseEntity.ok(approvalService.getPendingApprovals(managerId, pageable));
    }

    // ── POST /admin/approvals/{id}/approve ───────────────
    // Manager approves a timesheet or leave request
    @Operation(summary = "Approve Request", description = "Approves a specific pending request in the queue. You can optionally provide a remark.")
    @PostMapping("/approvals/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> approve(
            @PathVariable Long id,
            @RequestBody(required = false)
            ApprovalActionDTO request) {

        String remark = request != null
                ? request.getRemark() : null;
        return ResponseEntity.ok(
                approvalService.approveItem(id, remark));
    }

    // ── POST /admin/approvals/{id}/reject ────────────────
    // Manager rejects — remark is mandatory
    @Operation(summary = "Reject Request", description = "Rejects a specific pending request. A rejection remark is strictly mandatory.")
    @PostMapping("/approvals/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> reject(
            @PathVariable Long id,
            @RequestBody ApprovalActionDTO request) {

        return ResponseEntity.ok(
                approvalService.rejectItem(
                        id, request.getRemark()));
    }

    // ── POST /admin/approvals/queue ──────────────────────
    // Add item to approval queue
    // called when timesheet or leave submitted
    // will be replaced by RabbitMQ consumer in Sprint 3
    @Operation(summary = "Add to Approval Queue", description = "Manually enqueues a submitted timesheet or leave request for managerial review.")
    @PostMapping("/approvals/queue")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> addToQueue(
            @RequestParam Long referenceId,
            @RequestParam ReferenceType referenceType,
            @RequestParam Long requestedBy,
            @RequestParam Long assignedTo) {

        approvalService.addToQueue(
                referenceId, referenceType,
                requestedBy, assignedTo);
        return ResponseEntity.ok(
                "Added to approval queue");
    }

    // ── GET /admin/config/public ─────────────────────────
    // Public announcements — no JWT needed
    @Operation(summary = "Public Config Status", description = "Unauthenticated health check mapping for public announcements.")
    @GetMapping("/config/public")
    public ResponseEntity<String> getPublicConfig() {
        return ResponseEntity.ok(
                "System is operational");
    }

    // ── private helper ───────────────────────────────────
    private Long extractUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}