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
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

import com.cap.admin.exception.BusinessRuleException;
import com.cap.admin.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ApprovalQueueRepository approvalQueueRepository;
    private final RestTemplate            restTemplate;
    private final RabbitTemplate          rabbitTemplate;

    @Value("${admin.timesheet-service-url}")
    private String timesheetUrl;

    @Value("${admin.leave-service-url}")
    private String leaveUrl;

    // ════════════════════════════════════════════════
    // GET PENDING APPROVALS FOR MANAGER
    // ════════════════════════════════════════════════
    @Transactional
    public Page<ApprovalQueueResponseDTO> getPendingApprovals(Long managerId, Pageable pageable) {

        return approvalQueueRepository
                .findByAssignedToAndStatus(managerId, ApprovalStatus.PENDING,pageable)
                .map(item -> {
                    ApprovalQueueResponseDTO dto =
                        ApprovalQueueResponseDTO.builder()
                            .id(item.getId())
                            .referenceId(item.getReferenceId())
                            .referenceType(item.getReferenceType())
                            .requestedBy(item.getRequestedBy())
                            .status(item.getStatus())
                            .remark(item.getRemark())
                            .createdAt(item.getCreatedAt())
                            .build();

                    // enrich with details from other service
                    dto.setDetails(fetchDetails(item.getReferenceId(),item.getReferenceType()));
                    return dto;
                });
    }

    // ════════════════════════════════════════════════
    // APPROVE ITEM
    // ════════════════════════════════════════════════
    @Transactional
    public String approveItem(Long queueId, String remark) {

        ApprovalQueue item = approvalQueueRepository
                .findById(queueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Approval item not found: "
                                + queueId));

        // idempotency check
        if (item.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException(
                    "Item already " + item.getStatus()
                    + " — cannot approve again");
        }

        // update queue
        item.setStatus(ApprovalStatus.APPROVED);
        item.setRemark(remark);
        item.setActionedAt(LocalDateTime.now());
        approvalQueueRepository.save(item);

        // publish approve command to the correct service
        String routingKey = item.getReferenceType() == ReferenceType.TIMESHEET
                ? "timesheet.approve.command"
                : "leave.approve.command";

        ApproveCommandEvent cmd = ApproveCommandEvent.builder()
                .referenceId(item.getReferenceId())
                .approverId(item.getAssignedTo())
                .remark(remark)
                .action("APPROVE")
                .build();

        try {
            rabbitTemplate.convertAndSend("admin.commands", routingKey, cmd);
            log.info("[ADMIN] Published {} to {}", routingKey, item.getReferenceId());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish approve command: {}", e.getMessage());
        }

        // publish notification event
        String notifType = item.getReferenceType() == ReferenceType.TIMESHEET
                ? "TIMESHEET_APPROVED" : "LEAVE_APPROVED";
        String notifRk = item.getReferenceType() == ReferenceType.TIMESHEET
                ? "timesheet.approved" : "leave.approved";
        try {
            rabbitTemplate.convertAndSend("notification.events", notifRk,
                    NotificationEvent.builder()
                            .userId(item.getRequestedBy())
                            .type(notifType)
                            .title("Request Approved")
                            .body("Your " + item.getReferenceType().name().toLowerCase()
                                    + " request has been approved.")
                            .build());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish notification: {}", e.getMessage());
        }

        return "Item approved successfully";
    }

    // ════════════════════════════════════════════════
    // REJECT ITEM
    // ════════════════════════════════════════════════
    @Transactional
    public String rejectItem(Long queueId, String remark) {

        // remark is mandatory for rejection
        if (remark == null || remark.isBlank()) {
            throw new BusinessRuleException(
                    "Remark is mandatory for rejection");
        }

        ApprovalQueue item = approvalQueueRepository
                .findById(queueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Approval item not found: "
                                + queueId));

        if (item.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException(
                    "Item already " + item.getStatus()
                    + " — cannot reject again");
        }

        item.setStatus(ApprovalStatus.REJECTED);
        item.setRemark(remark);
        item.setActionedAt(LocalDateTime.now());
        approvalQueueRepository.save(item);

        // publish reject command to the correct service
        String routingKey = item.getReferenceType() == ReferenceType.TIMESHEET
                ? "timesheet.reject.command"
                : "leave.reject.command";

        ApproveCommandEvent cmd = ApproveCommandEvent.builder()
                .referenceId(item.getReferenceId())
                .approverId(item.getAssignedTo())
                .remark(remark)
                .action("REJECT")
                .build();

        try {
            rabbitTemplate.convertAndSend("admin.commands", routingKey, cmd);
            log.info("[ADMIN] Published {} to {}", routingKey, item.getReferenceId());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish reject command: {}", e.getMessage());
        }

        // publish notification event
        String notifRk = item.getReferenceType() == ReferenceType.TIMESHEET
                ? "timesheet.rejected" : "leave.rejected";
        try {
            rabbitTemplate.convertAndSend("notification.events", notifRk,
                    NotificationEvent.builder()
                            .userId(item.getRequestedBy())
                            .type(item.getReferenceType().name() + "_REJECTED")
                            .title("Request Rejected")
                            .body("Your " + item.getReferenceType().name().toLowerCase()
                                    + " request was rejected. Reason: " + remark)
                            .build());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish notification: {}", e.getMessage());
        }

        return "Item rejected successfully";
    }

    // ════════════════════════════════════════════════
    // ADD TO QUEUE
    // called when timesheet or leave is submitted
    // will be called by RabbitMQ consumer in Sprint 3
    // for now can be called directly via REST
    // ════════════════════════════════════════════════
    @Transactional
    public void addToQueue(Long referenceId,
                           ReferenceType referenceType,
                           Long requestedBy,
                           Long assignedTo) {

        // prevent duplicate queue entries
        boolean exists = approvalQueueRepository
                .existsByReferenceIdAndReferenceTypeAndStatus(
                        referenceId,
                        referenceType,
                        ApprovalStatus.PENDING);

        if (exists) {
            return; // already in queue — skip silently
        }

        ApprovalQueue queue = ApprovalQueue.builder()
                .referenceId(referenceId)
                .referenceType(referenceType)
                .requestedBy(requestedBy)
                .assignedTo(assignedTo)
                .status(ApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        approvalQueueRepository.save(queue);
    }

    // ════════════════════════════════════════════════
    // PRIVATE HELPER — fetch details from other service
    // ════════════════════════════════════════════════
    private Object fetchDetails(Long referenceId,
                                ReferenceType type) {
        try {
            if (type == ReferenceType.TIMESHEET) {
                return restTemplate.getForObject(
                        timesheetUrl
                        + "/timesheet/weeks/id/"
                        + referenceId,
                        TimesheetDetailsDTO.class);
            } else {
                return restTemplate.getForObject(
                        leaveUrl
                        + "/leave/requests/"
                        + referenceId,
                        LeaveDetailsDTO.class);
            }
        } catch (Exception e) {
            // if service is down return null gracefully
            return null;
        }
    }
}