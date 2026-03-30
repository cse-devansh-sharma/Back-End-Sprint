package com.cap.leave.service;

import com.cap.leave.dto.*;
import com.cap.leave.entity.*;
import com.cap.leave.enums.LeaveStatus;
import com.cap.leave.messaging.dto.ApproveCommandEvent;
import com.cap.leave.messaging.dto.LeaveSubmittedEvent;
import com.cap.leave.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.cap.leave.exception.BusinessRuleException;
import com.cap.leave.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRequestRepository  leaveRequestRepository;
    private final LeaveBalanceRepository  leaveBalanceRepository;
    private final LeaveTypeRepository     leaveTypeRepository;
    private final HolidayRepository       holidayRepository;
    private final LeaveAuditLogRepository leaveAuditLogRepository;
    private final RabbitTemplate          rabbitTemplate;

    @Value("${leave.max-cancellation-days-before}")
    private int cancellationDaysBefore;

    // ════════════════════════════════════════════════
    // CREATE LEAVE REQUEST
    // ════════════════════════════════════════════════
    @Transactional
    public LeaveRequestResponseDTO createLeaveRequest(LeaveRequestCreateDTO request, Long userId) {


        LeaveType leaveType = leaveTypeRepository
                .findById(request.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));

        if (!leaveType.getIsActive()) {
            throw new BusinessRuleException("Leave type is not active");
        }

        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new BusinessRuleException("From date cannot be after to date");
        }

        
        long daysUntilLeave = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(),
                request.getFromDate()
        );
        
        if (daysUntilLeave < leaveType.getMinNoticeDays()) {
            throw new BusinessRuleException("Leave must be applied at least " + leaveType.getMinNoticeDays() + " day(s) in advance");
        }

  
        if (leaveType.getRequiresDelegate() && request.getDelegateUserId() == null) {
            throw new BusinessRuleException("Delegate is required for this leave type");
        }

        List<LeaveStatus> activeStatuses = List.of(LeaveStatus.SUBMITTED, LeaveStatus.APPROVED);

        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingLeave(userId, request.getFromDate(),request.getToDate(), activeStatuses);

        if (!overlapping.isEmpty()) {
            throw new BusinessRuleException("Date range overlaps with an existing leave request");
        }

        BigDecimal numberOfDays = calculateWorkingDays(request.getFromDate(), request.getToDate());

        if (numberOfDays.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("Selected dates have no working days");
        }

        int currentYear = LocalDate.now().getYear();
        
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(
                        userId,
                        leaveType.getId(),
                        currentYear)
                .orElseThrow(() -> new ResourceNotFoundException("No leave balance found for this leave type"));

        BigDecimal remaining = balance.getTotalAllotted()
                .subtract(balance.getUsed())
                .subtract(balance.getPending());

        if (numberOfDays.compareTo(remaining) > 0) {
            throw new BusinessRuleException("Insufficient leave balance. Available: " + remaining + " days");
        }

        // ── Step 8: save leave request ────────────────────────
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .userId(userId)
                .leaveType(leaveType)
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .numberOfDays(numberOfDays)
                .reason(request.getReason())
                .delegateUserId(request.getDelegateUserId())
                .status(LeaveStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        leaveRequest = leaveRequestRepository.save(leaveRequest);

        balance.setPending(balance.getPending().add(numberOfDays));
        leaveBalanceRepository.save(balance);

        writeAuditLog(leaveRequest, "SUBMITTED", userId, null);

        // publish leave.submitted event to RabbitMQ
        try {
            rabbitTemplate.convertAndSend(
                    "leave.events",
                    "leave.submitted",
                    LeaveSubmittedEvent.builder()
                            .leaveRequestId(leaveRequest.getId())
                            .userId(userId)
                            .managerId(userId) // will be enriched by admin-service logic
                            .fromDate(request.getFromDate().toString())
                            .toDate(request.getToDate().toString())
                            .leaveTypeName(leaveType.getName())
                            .numberOfDays(numberOfDays.doubleValue())
                            .build());
            log.info("[LEAVE] Published leave.submitted for requestId={}", leaveRequest.getId());
        } catch (Exception e) {
            log.error("[LEAVE] Failed to publish leave.submitted: {}", e.getMessage());
        }

        return mapToResponseDTO(leaveRequest);
    }

    // ════════════════════════════════════════════════
    // APPROVE LEAVE
    // called by RabbitMQ consumer (admin.commands)
    // ════════════════════════════════════════════════
    @Transactional
    public void approveLeave(Long requestId,
                             Long approverId,
                             String remark) {

        LeaveRequest leaveRequest = leaveRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new com.cap.leave.exception.LeaveNotFoundException("Leave request not found with id: " + requestId));

        // only SUBMITTED requests can be approved
        if (leaveRequest.getStatus() != LeaveStatus.SUBMITTED) {
            throw new BusinessRuleException( "Only submitted requests can be approved");
        }

        // ── update status ─────────────────────────────────────
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setManagerId(approverId);
        leaveRequest.setManagerRemark(remark);
        leaveRequest.setActionedAt(LocalDateTime.now());
        leaveRequestRepository.save(leaveRequest);

        // ── move pending → used in balance ────────────────────
        int year = leaveRequest.getFromDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(
                        leaveRequest.getUserId(),
                        leaveRequest.getLeaveType().getId(),
                        year)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));


        balance.setPending( balance.getPending().subtract(leaveRequest.getNumberOfDays()));
        balance.setUsed(balance.getUsed().add(leaveRequest.getNumberOfDays()));
        leaveBalanceRepository.save(balance);

        writeAuditLog(leaveRequest, "APPROVED", approverId, remark);

        // publish leave.approved event to RabbitMQ
        try {
            rabbitTemplate.convertAndSend(
                    "leave.events",
                    "leave.approved",
                    ApproveCommandEvent.builder()
                            .referenceId(leaveRequest.getId())
                            .approverId(approverId)
                            .remark(remark)
                            .action("APPROVE")
                            .build());
            log.info("[LEAVE] Published leave.approved for requestId={}", leaveRequest.getId());
        } catch (Exception e) {
            log.error("[LEAVE] Failed to publish leave.approved: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════
    // REJECT LEAVE
    // called by RabbitMQ consumer (admin.commands)
    // ════════════════════════════════════════════════
    @Transactional
    public void rejectLeave(Long requestId, Long approverId, String remark) {

        LeaveRequest leaveRequest = leaveRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new com.cap.leave.exception.LeaveNotFoundException("Leave request not found with id: " + requestId));

        if (leaveRequest.getStatus() != LeaveStatus.SUBMITTED) {
            throw new BusinessRuleException("Only submitted requests can be rejected");
        }

        // ── update status ─────────────────────────────────────
        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setManagerId(approverId);
        leaveRequest.setManagerRemark(remark);
        leaveRequest.setActionedAt(LocalDateTime.now());
        leaveRequestRepository.save(leaveRequest);

        // ── release pending balance back to remaining ─────────
        int year = leaveRequest.getFromDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(leaveRequest.getUserId(),leaveRequest.getLeaveType().getId(),year)
                .orElseThrow(() ->new ResourceNotFoundException("Leave balance not found"));

        balance.setPending(balance.getPending()
               .subtract(leaveRequest.getNumberOfDays()));
        leaveBalanceRepository.save(balance);

        // ── write audit log ───────────────────────────────────
        writeAuditLog(leaveRequest, "REJECTED", approverId, remark);

        // publish leave.rejected event to RabbitMQ
        try {
            rabbitTemplate.convertAndSend(
                    "leave.events",
                    "leave.rejected",
                    ApproveCommandEvent.builder()
                            .referenceId(leaveRequest.getId())
                            .approverId(approverId)
                            .remark(remark)
                            .action("REJECT")
                            .build());
            log.info("[LEAVE] Published leave.rejected for requestId={}", leaveRequest.getId());
        } catch (Exception e) {
            log.error("[LEAVE] Failed to publish leave.rejected: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════
    // CANCEL LEAVE
    // ════════════════════════════════════════════════
    @Transactional
    public String cancelLeave(Long requestId, Long userId) {

        LeaveRequest leaveRequest = leaveRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new com.cap.leave.exception.LeaveNotFoundException("Leave request not found with id: " + requestId));

        // employee can only cancel their own request
        if (!leaveRequest.getUserId().equals(userId)) {
            throw new BusinessRuleException("You can only cancel your own leave request");
        }

        // only SUBMITTED or APPROVED can be cancelled
        if (leaveRequest.getStatus() != LeaveStatus.SUBMITTED && leaveRequest.getStatus() != LeaveStatus.APPROVED) {
            throw new BusinessRuleException("This request cannot be cancelled");
        }

        // ── check cancellation window ─────────────────────────
        // cannot cancel if leave starts today or has already started
        LocalDate today = LocalDate.now();
        long daysUntilLeave = today
                .until(leaveRequest.getFromDate()).getDays();

        if (daysUntilLeave < cancellationDaysBefore) {
            throw new BusinessRuleException(
                    "Cannot cancel leave less than "
                    + cancellationDaysBefore
                    + " day(s) before it starts");
        }

        // ── update status ─────────────────────────────────────
        LeaveStatus previousStatus = leaveRequest.getStatus();
        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequest.setActionedAt(LocalDateTime.now());
        leaveRequestRepository.save(leaveRequest);

        // ── release balance ───────────────────────────────────
        int year = leaveRequest.getFromDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(
                        leaveRequest.getUserId(),
                        leaveRequest.getLeaveType().getId(),
                        year)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

        if (previousStatus == LeaveStatus.SUBMITTED) {
            // was pending — release from pending
            balance.setPending(
                    balance.getPending()
                           .subtract(leaveRequest.getNumberOfDays()));
        } else {
            // was approved — release from used
            balance.setUsed(
                    balance.getUsed()
                           .subtract(leaveRequest.getNumberOfDays()));
        }
        leaveBalanceRepository.save(balance);

        // ── write audit log ───────────────────────────────────
        writeAuditLog(leaveRequest, "CANCELLED", userId, null);

        // publish leave.cancelled event
        try {
            rabbitTemplate.convertAndSend(
                    "notification.events",
                    "leave.cancelled",
                    java.util.Map.of(
                            "leaveRequestId", leaveRequest.getId(),
                            "cancelledBy",    userId));
        } catch (Exception e) {
            log.error("[LEAVE] Failed to publish leave.cancelled: {}", e.getMessage());
        }

        return "Leave request cancelled successfully";
    }

    // ════════════════════════════════════════════════
    // GET LEAVE HISTORY
    // ════════════════════════════════════════════════
    @Transactional
    public Page<LeaveRequestResponseDTO> getLeaveHistory(
            Long userId,
            Pageable pageable) {

        return leaveRequestRepository
                .findByUserId(userId, pageable)
                .map(this::mapToResponseDTO);
    }

    // ════════════════════════════════════════════════
    // GET LEAVE BALANCE
    // ════════════════════════════════════════════════
    @Transactional
    public List<LeaveBalanceDTO> getLeaveBalances(Long userId) {

        int currentYear = LocalDate.now().getYear();

        return leaveBalanceRepository
                .findByUserIdAndYear(userId, currentYear)
                .stream()
                .map(balance -> LeaveBalanceDTO.builder()
                        .leaveTypeCode( balance.getLeaveType().getCode())
                        .leaveTypeName( balance.getLeaveType().getName())
                        .totalAllotted(balance.getTotalAllotted())
                        .used(balance.getUsed())
                        .pending(balance.getPending())
                        .remaining(balance.getTotalAllotted()
                        .subtract(balance.getUsed())
                        .subtract(balance.getPending()))
                        .build())
                .collect(Collectors.toList());
    }
    
    @Transactional
    public LeaveRequestResponseDTO getLeaveRequestById(
            Long requestId, Long userId) {

        LeaveRequest leaveRequest = leaveRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new com.cap.leave.exception.LeaveNotFoundException( "Leave request not found with id: " + requestId));

        return mapToResponseDTO(leaveRequest);
    }
    
    @Transactional
    public LeaveStatusResponseDTO getLeaveStatusById(Long requestId, Long userId) {
        LeaveRequest leaveRequest = leaveRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new com.cap.leave.exception.LeaveNotFoundException( "Leave request not found with id: " + requestId));

        // Employee can only check their own leave status, managers/admins could check any
        if (leaveRequest.getUserId().equals(userId)) {
            return new LeaveStatusResponseDTO(leaveRequest.getId(), leaveRequest.getStatus());
        }
        
        // Return for others if authorization pass at controller
        return new LeaveStatusResponseDTO(leaveRequest.getId(), leaveRequest.getStatus());
    }
    // ════════════════════════════════════════════════
    // GET TEAM CALENDAR
    // ════════════════════════════════════════════════
    @Transactional
    public List<TeamCalendarDTO> getTeamCalendar(
            List<Long> userIds,
            LocalDate monthStart,
            LocalDate monthEnd) {

        // get all approved leaves for the team in the month
        List<LeaveRequest> teamLeaves = leaveRequestRepository
                .findByUserIdInAndStatus(
                        userIds,
                        LeaveStatus.APPROVED);

        // filter by date range
        return teamLeaves.stream()
                .filter(lr ->
                        !lr.getToDate().isBefore(monthStart)
                        && !lr.getFromDate().isAfter(monthEnd))
                .map(lr -> TeamCalendarDTO.builder()
                        .userId(lr.getUserId())
                        .leaveType(lr.getLeaveType().getName())
                        .status(lr.getStatus())
                        .fromDate(lr.getFromDate())
                        .toDate(lr.getToDate())
                        .build())
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════
    // GET HOLIDAYS
    // ════════════════════════════════════════════════
    @Transactional
    public List<HolidayDTO> getHolidaysByYear(Integer year) {
        return holidayRepository
                .findByYearAndIsActive(year, true)
                .stream()
                .map(h -> HolidayDTO.builder()
                        .id(h.getId())
                        .holidayDate(h.getHolidayDate())
                        .name(h.getName())
                        .type(h.getType())
                        .year(h.getYear())
                        .build())
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════
    // CHECK IF USER IS ON LEAVE
    // ════════════════════════════════════════════════
    @Transactional
    public boolean isOnLeave(Long userId, LocalDate date) {
        List<LeaveStatus> activeStatuses = List.of(LeaveStatus.APPROVED, LeaveStatus.SUBMITTED);
        List<LeaveRequest> overlapping = leaveRequestRepository
                .findOverlappingLeave(userId, date, date, activeStatuses);
        return !overlapping.isEmpty();
    }

    // ════════════════════════════════════════════════
    // ALLOCATE INITIAL LEAVES
    // ════════════════════════════════════════════════
    @Transactional
    public void allocateInitialLeaves(Long userId) {
        // Ensure default leave types (CL, SL) exist before attempting to create balances
        ensureDefaultLeaveTypesExist();

        int currentYear = LocalDate.now().getYear();
        List<LeaveType> activeTypes = leaveTypeRepository.findByIsActive(true);

        for (LeaveType leaveType : activeTypes) {
            BigDecimal defaultAllotted = BigDecimal.ZERO;
            if ("CL".equalsIgnoreCase(leaveType.getCode())) {
                defaultAllotted = new BigDecimal("12.00");
            } else if ("SL".equalsIgnoreCase(leaveType.getCode())) {
                defaultAllotted = new BigDecimal("12.00");
            }
            
            // Avoid creating duplicate records if re-registered or called twice
            boolean exists = leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(userId, leaveType.getId(), currentYear).isPresent();
            if (!exists) {
                LeaveBalance balance = LeaveBalance.builder()
                        .userId(userId)
                        .leaveType(leaveType)
                        .year(currentYear)
                        .totalAllotted(defaultAllotted)
                        .used(BigDecimal.ZERO)
                        .pending(BigDecimal.ZERO)
                        .carryForwarded(BigDecimal.ZERO)
                        .build();

                leaveBalanceRepository.save(balance);
                log.info("[LEAVE] Allocated {} init leaves for code:{} user:{}", defaultAllotted, leaveType.getCode(), userId);
            }
        }
    }

    // ════════════════════════════════════════════════
    // MANUAL ALLOCATE LEAVES (Internal / Authority)
    // ════════════════════════════════════════════════
    @Transactional
    public void manualAllocateLeaves(LeaveAllocationRequestDTO request) {
        LeaveType leaveType = leaveTypeRepository.findByCodeAndIsActive(request.getLeaveTypeCode(), true)
                .orElseThrow(() -> new ResourceNotFoundException("Active leave type not found with code: " + request.getLeaveTypeCode()));

        int year = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
        
        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(
                request.getUserId(), leaveType.getId(), year)
                .orElseGet(() -> {
                    log.info("[LEAVE] Creating new balance for user:{} type:{} year:{}", request.getUserId(), request.getLeaveTypeCode(), year);
                    return LeaveBalance.builder()
                            .userId(request.getUserId())
                            .leaveType(leaveType)
                            .year(year)
                            .totalAllotted(BigDecimal.ZERO)
                            .used(BigDecimal.ZERO)
                            .pending(BigDecimal.ZERO)
                            .carryForwarded(BigDecimal.ZERO)
                            .build();
                });

        BigDecimal previousAllotted = balance.getTotalAllotted();
        balance.setTotalAllotted(previousAllotted.add(request.getAmount()));
        leaveBalanceRepository.save(balance);

        log.info("[LEAVE] Manually increased leave balance for user:{} type:{} from {} to {}", 
                request.getUserId(), request.getLeaveTypeCode(), previousAllotted, balance.getTotalAllotted());
        
        // Manual audit log entry (id is null since it's not a request action)
        writeAuditLog(null, "MANUAL_ALLOCATION", request.getUserId(), 
                "Added " + request.getAmount() + " days to " + request.getLeaveTypeCode());
    }

    private void ensureDefaultLeaveTypesExist() {
        createDefaultTypeIfNotExists("CL", "Casual Leave");
        createDefaultTypeIfNotExists("SL", "Sick Leave");
    }

    private void createDefaultTypeIfNotExists(String code, String name) {
        if (leaveTypeRepository.findByCodeAndIsActive(code, true).isEmpty()) {
            LeaveType leaveType = LeaveType.builder()
                    .code(code)
                    .name(name)
                    .isPaid(true)
                    .isActive(true)
                    .requiresDelegate(false)
                    .minNoticeDays(0)
                    .allowHalfDay(true)
                    .carryForwardAllowed(false)
                    .maxCarryForwardDays(0)
                    .build();
            leaveTypeRepository.save(leaveType);
            log.info("[LEAVE] Auto-initialized default leave type: {} ({})", code, name);
        }
    }

    // ════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════

    // calculates working days excluding weekends and holidays
    private BigDecimal calculateWorkingDays(
            LocalDate fromDate,
            LocalDate toDate) {

        // fetch holidays in the date range
        List<LocalDate> holidays = holidayRepository
                .findByHolidayDateBetweenAndIsActive(
                        fromDate, toDate, true)
                .stream()
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toList());

        int workingDays = 0;
        LocalDate current = fromDate;

        while (!current.isAfter(toDate)) {
            // skip Saturday and Sunday
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY
                    && current.getDayOfWeek() != DayOfWeek.SUNDAY
                    && !holidays.contains(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return BigDecimal.valueOf(workingDays);
    }

    // writes a record to leave_audit_logs
    private void writeAuditLog(LeaveRequest request, String action, Long performedBy, String remarks) {
        LeaveAuditLog log = LeaveAuditLog.builder()
                .leaveRequest(request)
                .action(action)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .remarks(remarks)
                .build();

        leaveAuditLogRepository.save(log);
    }

    // maps entity to response DTO
    private LeaveRequestResponseDTO mapToResponseDTO(LeaveRequest lr) {
        return LeaveRequestResponseDTO.builder()
                .id(lr.getId())
                .leaveTypeName(lr.getLeaveType().getName())
                .leaveTypeCode(lr.getLeaveType().getCode())
                .fromDate(lr.getFromDate())
                .toDate(lr.getToDate())
                .numberOfDays(lr.getNumberOfDays())
                .status(lr.getStatus())
                .reason(lr.getReason())
                .managerRemark(lr.getManagerRemark())
                .submittedAt(lr.getSubmittedAt())
                .build();
    }
}