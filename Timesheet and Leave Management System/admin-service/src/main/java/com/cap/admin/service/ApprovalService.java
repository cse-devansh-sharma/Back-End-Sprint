package com.cap.admin.service;

import com.cap.admin.dto.*;
import com.cap.admin.entity.ApprovalQueue;
import com.cap.admin.enums.ApprovalStatus;
import com.cap.admin.enums.ReferenceType;
import com.cap.admin.messaging.dto.ApproveCommandEvent;
import com.cap.admin.messaging.dto.NotificationEvent;
import com.cap.admin.repository.ApprovalQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cap.admin.client.LeaveServiceClient;
import com.cap.admin.client.TimesheetServiceClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.cap.admin.exception.BusinessRuleException;
import com.cap.admin.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ApprovalQueueRepository approvalQueueRepository;
    private final TimesheetServiceClient  timesheetServiceClient;
    private final LeaveServiceClient      leaveServiceClient;
    private final RabbitTemplate          rabbitTemplate;

    @Transactional
    public Page<ApprovalQueueResponseDTO> getPendingApprovals(Long managerId, String role, Pageable pageable) {
        log.info("[ADMIN] Fetching approvals for managerId={} role={}", managerId, role);
        Page<ApprovalQueue> items;
        if ("ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
            items = approvalQueueRepository.findByStatus(ApprovalStatus.PENDING, pageable);
        } else {
            items = approvalQueueRepository.findByAssignedToAndStatus(managerId, ApprovalStatus.PENDING, pageable);
        }
        return items.map(item -> {
            ApprovalQueueResponseDTO dto = ApprovalQueueResponseDTO.builder()
                    .id(item.getId())
                    .referenceId(item.getReferenceId())
                    .referenceType(item.getReferenceType())
                    .requestedBy(item.getRequestedBy())
                    .status(item.getStatus())
                    .remark(item.getRemark())
                    .createdAt(item.getCreatedAt())
                    .build();
            dto.setDetails(fetchDetails(item.getReferenceId(), item.getReferenceType()));
            return dto;
        });
    }

    @Transactional
    public String approveItem(Long queueId, String remark) {
        ApprovalQueue item = approvalQueueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval item not found: " + queueId));
        if (item.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException("Item already " + item.getStatus() + " — cannot approve again");
        }
        item.setStatus(ApprovalStatus.APPROVED);
        item.setRemark(remark);
        item.setActionedAt(LocalDateTime.now());
        approvalQueueRepository.save(item);

        String routingKey = item.getReferenceType() == ReferenceType.TIMESHEET
                ? "timesheet.approve.command" : "leave.approve.command";
        ApproveCommandEvent cmd = ApproveCommandEvent.builder()
                .referenceId(item.getReferenceId())
                .approverId(item.getAssignedTo())
                .remark(remark).action("APPROVE").build();
        try {
            rabbitTemplate.convertAndSend("admin.commands", routingKey, cmd);
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish approve command: {}", e.getMessage());
        }
        try {
            String notifRk = item.getReferenceType() == ReferenceType.TIMESHEET ? "timesheet.approved" : "leave.approved";
            rabbitTemplate.convertAndSend("notification.events", notifRk,
                    NotificationEvent.builder().userId(item.getRequestedBy())
                            .type(item.getReferenceType().name() + "_APPROVED")
                            .title("Request Approved")
                            .body("Your " + item.getReferenceType().name().toLowerCase() + " request has been approved.")
                            .build());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish notification: {}", e.getMessage());
        }
        return "Item approved successfully";
    }

    @Transactional
    public String rejectItem(Long queueId, String remark) {
        if (remark == null || remark.isBlank()) {
            throw new BusinessRuleException("Remark is mandatory for rejection");
        }
        ApprovalQueue item = approvalQueueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval item not found: " + queueId));
        if (item.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException("Item already " + item.getStatus() + " — cannot reject again");
        }
        item.setStatus(ApprovalStatus.REJECTED);
        item.setRemark(remark);
        item.setActionedAt(LocalDateTime.now());
        approvalQueueRepository.save(item);

        String routingKey = item.getReferenceType() == ReferenceType.TIMESHEET
                ? "timesheet.reject.command" : "leave.reject.command";
        ApproveCommandEvent cmd = ApproveCommandEvent.builder()
                .referenceId(item.getReferenceId())
                .approverId(item.getAssignedTo())
                .remark(remark).action("REJECT").build();
        try {
            rabbitTemplate.convertAndSend("admin.commands", routingKey, cmd);
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish reject command: {}", e.getMessage());
        }
        try {
            String notifRk = item.getReferenceType() == ReferenceType.TIMESHEET ? "timesheet.rejected" : "leave.rejected";
            rabbitTemplate.convertAndSend("notification.events", notifRk,
                    NotificationEvent.builder().userId(item.getRequestedBy())
                            .type(item.getReferenceType().name() + "_REJECTED")
                            .title("Request Rejected")
                            .body("Your " + item.getReferenceType().name().toLowerCase() + " request was rejected. Reason: " + remark)
                            .build());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish notification: {}", e.getMessage());
        }
        return "Item rejected successfully";
    }

    @Transactional
    public void addToQueue(Long referenceId, ReferenceType referenceType, Long requestedBy, Long assignedTo) {
        boolean exists = approvalQueueRepository.existsByReferenceIdAndReferenceTypeAndStatus(
                referenceId, referenceType, ApprovalStatus.PENDING);
        if (exists) return;
        ApprovalQueue queue = ApprovalQueue.builder()
                .referenceId(referenceId).referenceType(referenceType)
                .requestedBy(requestedBy).assignedTo(assignedTo)
                .status(ApprovalStatus.PENDING).createdAt(LocalDateTime.now()).build();
        approvalQueueRepository.save(queue);
    }

    // Returns distinct employee IDs who have submitted to this manager
    public List<Long> getTeamMemberIds(Long managerId) {
        return approvalQueueRepository.findByRequestedBy(managerId)
                .stream()
                .map(ApprovalQueue::getRequestedBy)
                .distinct()
                .collect(Collectors.toList());
    }

    private Object fetchDetails(Long referenceId, ReferenceType type) {
        try {
            if (type == ReferenceType.TIMESHEET) {
                return timesheetServiceClient.getTimesheetById(referenceId);
            } else {
                return leaveServiceClient.getLeaveRequestById(referenceId);
            }
        } catch (Exception e) {
            log.error("[ADMIN] Could not fetch details for type {} and id {}: {}", type, referenceId, e.getMessage());
            return null;
        }
    }
}
