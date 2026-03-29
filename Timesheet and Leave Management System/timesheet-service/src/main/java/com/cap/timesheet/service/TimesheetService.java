package com.cap.timesheet.service;

import com.cap.timesheet.dto.*;
import com.cap.timesheet.entity.*;
import com.cap.timesheet.enums.TimesheetStatus;
import com.cap.timesheet.messaging.dto.ApproveCommandEvent;
import com.cap.timesheet.messaging.dto.TimesheetSubmittedEvent;
import com.cap.timesheet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cap.timesheet.client.LeaveServiceClient;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.cap.timesheet.exception.BusinessRuleException;
import com.cap.timesheet.exception.HolidayClashException;
import com.cap.timesheet.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimesheetService {

    private final WeeklyTimesheetRepository weeklyTimesheetRepository;
    private final TimesheetEntryRepository  timesheetEntryRepository;
    private final ProjectRepository         projectRepository;
    private final TimesheetAuditLogRepository auditLogRepository;

    @Value("${timesheet.max-hours-per-day}")
    private int maxHoursPerDay;

    @Value("${timesheet.min-hours-per-week}")
    private int minHoursPerWeek;

    // Feign Client for calling Leave Service
    private final LeaveServiceClient leaveServiceClient;

    // RabbitMQ for event publishing
    private final RabbitTemplate rabbitTemplate;

    // ════════════════════════════════════════════════
    // SAVE ENTRY
    // ════════════════════════════════════════════════
    @Transactional
    public TimesheetEntryResponseDTO saveEntry(
            TimesheetEntryCreateDTO request,
            Long userId) {

        // ── Step 1: validate work date is not weekend ─────
        LocalDate workDate = request.getWorkDate();
        if (workDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException(
                    "Cannot log hours for future dates");
        }

        if (workDate.getDayOfWeek() == DayOfWeek.SATURDAY
                || workDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessRuleException(
                    "Cannot log hours on a weekend");
        }

        // ── Step 2: check if work date is a holiday ───────
        // calls Leave Service GET /leave/holidays?year=YYYY
        if (isHoliday(workDate)) {
            throw new HolidayClashException(
                    "Cannot log hours on a public holiday: "
                    + workDate);
        }

        // ── Step 2.5: check if employee is on leave ───────
        if (isOnLeave(userId, workDate)) {
            throw new BusinessRuleException(
                    "Cannot log hours while on approved leave: "
                    + workDate);
        }

        // ── Step 3: validate hours ─────────────────────────
        if (request.getHoursLogged() < 0) {
            throw new BusinessRuleException(
                    "Hours cannot be negative");
        }
        if (request.getHoursLogged() > maxHoursPerDay) {
            throw new BusinessRuleException(
                    "Hours cannot exceed " + maxHoursPerDay
                    + " per day");
        }

        // ── Step 4: validate project exists and is active ──
        Project project = projectRepository
                .findByIdAndIsActive(request.getProjectId(), true)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found or inactive"));

        // ── Step 5: check duplicate (userId+projectId+date) ─
        if (timesheetEntryRepository
                .existsByUserIdAndProjectIdAndWorkDate(
                        userId,
                        request.getProjectId(),
                        workDate)) {
            throw new BusinessRuleException(
                    "Entry already exists for this project on "
                    + workDate);
        }

        // ── Step 6: get or create WeeklyTimesheet ──────────
        LocalDate weekStart = getWeekStart(workDate);
        WeeklyTimesheet weeklyTimesheet = weeklyTimesheetRepository
                .findByUserIdAndWeekStart(userId, weekStart)
                .orElseGet(() -> {
                    // create new DRAFT timesheet for this week
                    WeeklyTimesheet newSheet = WeeklyTimesheet.builder()
                            .userId(userId)
                            .weekStart(weekStart)
                            .totalHours(BigDecimal.ZERO)
                            .status(TimesheetStatus.DRAFT)
                            .build();
                    return weeklyTimesheetRepository.save(newSheet);
                });

        // ── Step 7: block if week already submitted ────────
        if (weeklyTimesheet.getStatus() != TimesheetStatus.DRAFT
                && weeklyTimesheet.getStatus()
                        != TimesheetStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Cannot add entries to a "
                    + weeklyTimesheet.getStatus()
                    + " timesheet");
        }

        // ── Step 8: save the entry ─────────────────────────
        TimesheetEntry entry = TimesheetEntry.builder()
                .weeklyTimesheet(weeklyTimesheet)
                .userId(userId)
                .project(project)
                .workDate(workDate)
                .hoursLogged(BigDecimal.valueOf(
                        request.getHoursLogged()))
                .taskSummary(request.getTaskSummary())
                .build();

        timesheetEntryRepository.save(entry);

        // ── Step 9: recompute totalHours ───────────────────
        recomputeTotalHours(weeklyTimesheet);

        return mapToEntryResponseDTO(entry);
    }

    // ════════════════════════════════════════════════
    // GET WEEKLY TIMESHEET
    // ════════════════════════════════════════════════
    @Transactional
    public WeeklyTimesheetDTO getWeeklyTimesheet(
            LocalDate weekStart, Long userId) {

        // weekStart must be a Monday
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessRuleException(
                    "weekStart must be a Monday");
        }

        WeeklyTimesheet sheet = weeklyTimesheetRepository
                .findByUserIdAndWeekStart(userId, weekStart)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No timesheet found for week: "
                                + weekStart));

        return mapToWeeklyTimesheetDTO(sheet);
    }

    @Transactional
    public WeeklyTimesheetDTO getWeeklyTimesheetById(Long id) {
        WeeklyTimesheet sheet = weeklyTimesheetRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Timesheet not found with id: " + id));

        return mapToWeeklyTimesheetDTO(sheet);
    }

    private WeeklyTimesheetDTO mapToWeeklyTimesheetDTO(WeeklyTimesheet sheet) {
        List<TimesheetEntryResponseDTO> entries =
                timesheetEntryRepository
                        .findByWeeklyTimesheetIdOrderByWorkDateAsc(
                                sheet.getId())
                        .stream()
                        .map(this::mapToEntryResponseDTO)
                        .collect(Collectors.toList());

        return WeeklyTimesheetDTO.builder()
                .id(sheet.getId())
                .weekStart(sheet.getWeekStart())
                .totalHours(sheet.getTotalHours())
                .status(sheet.getStatus())
                .employeeComment(sheet.getEmployeeComment())
                .managerRemark(sheet.getManagerRemark())
                .entries(entries)
                .build();
    }

    // ════════════════════════════════════════════════
    // DELETE ENTRY
    // ════════════════════════════════════════════════
    @Transactional
    public String deleteEntry(Long projectId, Long entryId, LocalDate date, Long userId) {

        if (entryId == null && date == null) {
            throw new BusinessRuleException("Must provide either entryId or date to delete an entry");
        }

        TimesheetEntry entry;

        if (entryId != null) {
            entry = timesheetEntryRepository.findById(entryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Entry not found with id: " + entryId));
            
            if (!entry.getProject().getId().equals(projectId)) {
                throw new BusinessRuleException("Entry does not match the provided projectId");
            }
        } else {
            entry = timesheetEntryRepository.findByUserIdAndProjectIdAndWorkDate(userId, projectId, date)
                    .orElseThrow(() -> new ResourceNotFoundException("Entry not found for project " + projectId + " on date " + date));
        }

        // can only delete own entries
        if (!entry.getUserId().equals(userId)) {
            throw new BusinessRuleException(
                    "You can only delete your own entries");
        }

        // block deletion if week is submitted or beyond
        TimesheetStatus status = entry
                .getWeeklyTimesheet().getStatus();
        if (status != TimesheetStatus.DRAFT
                && status != TimesheetStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Cannot delete entry from a "
                    + status + " timesheet");
        }

        WeeklyTimesheet sheet = entry.getWeeklyTimesheet();
        timesheetEntryRepository.delete(entry);

        // recompute total after deletion
        recomputeTotalHours(sheet);

        return "Entry deleted successfully";
    }

    // ════════════════════════════════════════════════
    // VALIDATE WEEK
    // ════════════════════════════════════════════════
    @Transactional
    public ValidationResultDTO validateWeek(
            LocalDate weekStart, Long userId) {

        List<String> violations  = new ArrayList<>();
        List<LocalDate> missingDates = new ArrayList<>();

        WeeklyTimesheet sheet = weeklyTimesheetRepository
                .findByUserIdAndWeekStart(userId, weekStart)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No timesheet found for week: "
                                + weekStart));

        // get all entries for this week
        List<TimesheetEntry> entries = timesheetEntryRepository
                .findByWeeklyTimesheetIdOrderByWorkDateAsc(
                        sheet.getId());

        // get dates that have entries
        List<LocalDate> datesWithEntries = entries.stream()
                .map(TimesheetEntry::getWorkDate)
                .collect(Collectors.toList());

        // check Mon to Fri — each must have at least one entry
        LocalDate day = weekStart;
        for (int i = 0; i < 5; i++) {
            // skip if it's a holiday
            if (!isHoliday(day) && !datesWithEntries.contains(day)) {
                missingDates.add(day);
                violations.add("Missing entry for: " + day
                        + " (" + day.getDayOfWeek() + ")");
            }
            day = day.plusDays(1);
        }

        // check total hours
        if (sheet.getTotalHours().compareTo(
                BigDecimal.valueOf(minHoursPerWeek)) < 0
                && missingDates.isEmpty()) {
            violations.add("Total hours "
                    + sheet.getTotalHours()
                    + " is below minimum required "
                    + minHoursPerWeek);
        }

        return ValidationResultDTO.builder()
                .isValid(violations.isEmpty())
                .missingDates(missingDates)
                .violations(violations)
                .totalHours(sheet.getTotalHours())
                .build();
    }

    // ════════════════════════════════════════════════
    // SUBMIT WEEK
    // ════════════════════════════════════════════════
    @Transactional
    public String submitWeek(
            LocalDate weekStart,
            String comment,
            Long userId) {

        WeeklyTimesheet sheet = weeklyTimesheetRepository
                .findByUserIdAndWeekStart(userId, weekStart)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No timesheet found for week: "
                                + weekStart));

        // block if already submitted
        if (sheet.getStatus() == TimesheetStatus.SUBMITTED
                || sheet.getStatus() == TimesheetStatus.APPROVED
                || sheet.getStatus() == TimesheetStatus.LOCKED) {
            throw new BusinessRuleException(
                    "Timesheet already "
                    + sheet.getStatus());
        }

        // validate before submitting
        ValidationResultDTO validation =
                validateWeek(weekStart, userId);
        if (!validation.isValid()) {
            throw new BusinessRuleException(
                    "Timesheet has validation errors: "
                    + String.join(", ",
                            validation.getViolations()));
        }

        // update status
        sheet.setStatus(TimesheetStatus.SUBMITTED);
        sheet.setSubmittedAt(LocalDateTime.now());
        sheet.setEmployeeComment(comment);
        weeklyTimesheetRepository.save(sheet);

        // write audit log
        writeAuditLog(sheet, "SUBMITTED", userId, null);

        // publish timesheet.submitted event to RabbitMQ
        try {
            rabbitTemplate.convertAndSend(
                    "timesheet.events",
                    "timesheet.submitted",
                    TimesheetSubmittedEvent.builder()
                            .timesheetId(sheet.getId())
                            .userId(userId)
                            .managerId(sheet.getActionedBy() != null ? sheet.getActionedBy() : userId)
                            .weekStart(weekStart.toString())
                            .totalHours(sheet.getTotalHours() != null
                                    ? sheet.getTotalHours().doubleValue() : 0.0)
                            .build());
            log.info("[TIMESHEET] Published timesheet.submitted for id={}", sheet.getId());
        } catch (Exception e) {
            log.error("[TIMESHEET] Failed to publish submitted event: {}", e.getMessage());
        }

        return "Timesheet submitted successfully";
    }

    // ════════════════════════════════════════════════
    // APPROVE TIMESHEET
    // called by RabbitMQ consumer later
    // ════════════════════════════════════════════════
    @Transactional
    public void approveTimesheet(
            Long timesheetId,
            Long approverId,
            String remark) {

        WeeklyTimesheet sheet = weeklyTimesheetRepository
                .findById(timesheetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Timesheet not found"));

        // idempotency check
        if (sheet.getStatus() == TimesheetStatus.APPROVED) {
            return; // already approved, silently skip
        }

        if (sheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BusinessRuleException(
                    "Only submitted timesheets can be approved");
        }

        sheet.setStatus(TimesheetStatus.APPROVED);
        sheet.setManagerRemark(remark);
        sheet.setActionedBy(approverId);
        sheet.setActionedAt(LocalDateTime.now());
        weeklyTimesheetRepository.save(sheet);

        writeAuditLog(sheet, "APPROVED", approverId, remark);

        // publish timesheet.approved event
        try {
            rabbitTemplate.convertAndSend(
                    "timesheet.events",
                    "timesheet.approved",
                    ApproveCommandEvent.builder()
                            .referenceId(sheet.getId())
                            .approverId(approverId)
                            .remark(remark)
                            .action("APPROVE")
                            .build());
            log.info("[TIMESHEET] Published timesheet.approved for id={}", sheet.getId());
        } catch (Exception e) {
            log.error("[TIMESHEET] Failed to publish approved event: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════
    // REJECT TIMESHEET
    // called by RabbitMQ consumer later
    // ════════════════════════════════════════════════
    @Transactional
    public void rejectTimesheet(
            Long timesheetId,
            Long approverId,
            String remark) {

        WeeklyTimesheet sheet = weeklyTimesheetRepository
                .findById(timesheetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Timesheet not found"));

        if (sheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BusinessRuleException(
                    "Only submitted timesheets can be rejected");
        }

        sheet.setStatus(TimesheetStatus.REJECTED);
        sheet.setManagerRemark(remark);
        sheet.setActionedBy(approverId);
        sheet.setActionedAt(LocalDateTime.now());
        weeklyTimesheetRepository.save(sheet);

        writeAuditLog(sheet, "REJECTED", approverId, remark);

        // TODO: publish timesheet.rejected event to RabbitMQ
    }

    // ════════════════════════════════════════════════
    // GET TIMESHEET HISTORY
    // ════════════════════════════════════════════════
    @Transactional
    public Page<WeeklyTimesheetDTO> getHistory(
            Long userId, Pageable pageable, Long projectId) {

        return weeklyTimesheetRepository
                .findByUserIdOrderByWeekStartDesc(userId, pageable)
                .map(sheet -> {
                    List<TimesheetEntry> rawEntries = timesheetEntryRepository
                            .findByWeeklyTimesheetIdOrderByWorkDateAsc(sheet.getId());
                    
                    if (projectId != null) {
                        rawEntries = rawEntries.stream()
                                .filter(e -> e.getProject().getId().equals(projectId))
                                .collect(Collectors.toList());
                    }

                    List<TimesheetEntryResponseDTO> entries = rawEntries.stream()
                                    .map(this::mapToEntryResponseDTO)
                                    .collect(Collectors.toList());

                    return WeeklyTimesheetDTO.builder()
                            .id(sheet.getId())
                            .weekStart(sheet.getWeekStart())
                            .totalHours(sheet.getTotalHours())
                            .status(sheet.getStatus())
                            .employeeComment(
                                    sheet.getEmployeeComment())
                            .managerRemark(sheet.getManagerRemark())
                            .entries(entries)
                            .build();
                });
    }

    // ════════════════════════════════════════════════
    // GET ACTIVE PROJECTS
    // ════════════════════════════════════════════════
    public List<ProjectDTO> getActiveProjects() {
        return projectRepository.findByIsActive(true)
                .stream()
                .map(p -> ProjectDTO.builder()
                        .id(p.getId())
                        .projectCode(p.getProjectCode())
                        .name(p.getName())
                        .costCenter(p.getCostCenter())
                        .isBillable(p.getIsBillable())
                        .isActive(p.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════

    // get Monday of the week for a given date
    private LocalDate getWeekStart(LocalDate date) {
        return date.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // recompute total hours on the weekly timesheet
    private void recomputeTotalHours(WeeklyTimesheet sheet) {
        List<TimesheetEntry> entries = timesheetEntryRepository
                .findByWeeklyTimesheetIdOrderByWorkDateAsc(
                        sheet.getId());

        BigDecimal total = entries.stream()
                .map(TimesheetEntry::getHoursLogged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        sheet.setTotalHours(total);
        weeklyTimesheetRepository.save(sheet);
    }

    // check if a date is a public holiday
    // calls Leave Service via Feign
    private boolean isHoliday(LocalDate date) {
        try {
            List<HolidayDTO> holidays = leaveServiceClient.getHolidays(date.getYear());
            if (holidays == null) return false;
            return holidays.stream()
                    .anyMatch(h -> h.getHolidayDate().equals(date));
        } catch (Exception e) {
            log.error("Could not reach Leave Service for holiday check: {}", e.getMessage());
        }
        return false;
    }

    // check if employee is on leave
    // calls Leave Service via Feign
    private boolean isOnLeave(Long userId, LocalDate date) {
        try {
            OnLeaveStatusResponseDTO response = leaveServiceClient.isOnLeave(userId, date);
            return response != null && response.isOnLeave();
        } catch (Exception e) {
            log.error("Could not reach Leave Service for on-leave check: {}", e.getMessage());
        }
        return false;
    }

    // write audit log entry
    private void writeAuditLog(WeeklyTimesheet sheet,
            String action,
            Long performedBy,
            String remarks) {
TimesheetAuditLog log = TimesheetAuditLog.builder()
.timesheetId(sheet.getId())  // ← just pass the ID
.action(action)
.performedBy(performedBy)
.performedAt(LocalDateTime.now())
.remarks(remarks)
.build();
auditLogRepository.save(log);
}

    // map entry entity to response DTO
    private TimesheetEntryResponseDTO mapToEntryResponseDTO(
            TimesheetEntry entry) {
        return TimesheetEntryResponseDTO.builder()
                .id(entry.getId())
                .projectId(entry.getProject().getId())
                .projectName(entry.getProject().getName())
                .workDate(entry.getWorkDate())
                .hoursLogged(entry.getHoursLogged())
                .taskSummary(entry.getTaskSummary())
                .build();
    }
}