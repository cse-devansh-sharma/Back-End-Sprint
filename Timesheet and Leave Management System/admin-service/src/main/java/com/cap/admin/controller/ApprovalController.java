package com.cap.admin.controller;

import com.cap.admin.dto.*;
import com.cap.admin.enums.ReferenceType;
import com.cap.admin.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(summary = "Get Pending Approvals")
    @GetMapping("/approvals")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<ApprovalQueueResponseDTO>> getPendingApprovals(Authentication authentication, Pageable pageable) {
        Long managerId = extractUserId(authentication);
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (role.startsWith("ROLE_")) role = role.substring(5);
        return ResponseEntity.ok(approvalService.getPendingApprovals(managerId, role, pageable));
    }

    @Operation(summary = "Approve Request")
    @PostMapping("/approvals/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> approve(@PathVariable Long id, @RequestBody(required = false) ApprovalActionDTO request) {
        String remark = request != null ? request.getRemark() : null;
        return ResponseEntity.ok(approvalService.approveItem(id, remark));
    }

    @Operation(summary = "Reject Request")
    @PostMapping("/approvals/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> reject(@PathVariable Long id, @RequestBody ApprovalActionDTO request) {
        return ResponseEntity.ok(approvalService.rejectItem(id, request.getRemark()));
    }

    @Operation(summary = "Add to Approval Queue")
    @PostMapping("/approvals/queue")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> addToQueue(
            @RequestParam Long referenceId,
            @RequestParam ReferenceType referenceType,
            @RequestParam Long requestedBy,
            @RequestParam Long assignedTo) {
        approvalService.addToQueue(referenceId, referenceType, requestedBy, assignedTo);
        return ResponseEntity.ok("Added to approval queue");
    }

    @Operation(summary = "Public Config Status")
    @GetMapping("/config/public")
    public ResponseEntity<String> getPublicConfig() {
        return ResponseEntity.ok("System is operational");
    }

    // Returns distinct employee IDs who have submitted to this manager
    @Operation(summary = "Get Team Member IDs")
    @GetMapping("/team/members")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Long>> getTeamMembers(Authentication authentication) {
        Long managerId = extractUserId(authentication);
        return ResponseEntity.ok(approvalService.getTeamMemberIds(managerId));
    }

    private Long extractUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
