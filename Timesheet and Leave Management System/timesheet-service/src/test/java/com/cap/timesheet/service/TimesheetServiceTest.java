package com.cap.timesheet.service;

import com.cap.timesheet.client.LeaveServiceClient;
import com.cap.timesheet.dto.*;
import com.cap.timesheet.entity.*;
import com.cap.timesheet.enums.TimesheetStatus;
import com.cap.timesheet.messaging.dto.ApproveCommandEvent;
import com.cap.timesheet.messaging.dto.TimesheetSubmittedEvent;
import com.cap.timesheet.exception.BusinessRuleException;
import com.cap.timesheet.exception.HolidayClashException;
import com.cap.timesheet.exception.ResourceNotFoundException;
import com.cap.timesheet.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceTest {

    @Mock
    private WeeklyTimesheetRepository weeklyTimesheetRepository;
    @Mock
    private TimesheetEntryRepository timesheetEntryRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TimesheetAuditLogRepository auditLogRepository;
    @Mock
    private LeaveServiceClient leaveServiceClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TimesheetService timesheetService;

    private Long userId = 1L;
    private Project project;
    private WeeklyTimesheet weeklyTimesheet;
    private TimesheetEntry timesheetEntry;
    private LocalDate monday;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(timesheetService, "maxHoursPerDay", 8);
        ReflectionTestUtils.setField(timesheetService, "minHoursPerWeek", 40);

        monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        project = Project.builder()
                .id(1L)
                .projectCode("PRJ001")
                .name("Test Project")
                .isActive(true)
                .build();

        weeklyTimesheet = WeeklyTimesheet.builder()
                .id(1L)
                .userId(userId)
                .weekStart(monday)
                .status(TimesheetStatus.DRAFT)
                .totalHours(BigDecimal.ZERO)
                .build();

        timesheetEntry = TimesheetEntry.builder()
                .id(1L)
                .weeklyTimesheet(weeklyTimesheet)
                .userId(userId)
                .project(project)
                .workDate(monday)
                .hoursLogged(BigDecimal.valueOf(8))
                .build();
    }

    @Test
    void saveEntry_Success() {
        TimesheetEntryCreateDTO request = new TimesheetEntryCreateDTO();
        request.setProjectId(1L);
        request.setWorkDate(monday);
        request.setHoursLogged(8.0);
        request.setTaskSummary("Coding");

        when(leaveServiceClient.getHolidays(anyInt())).thenReturn(Collections.emptyList());
        OnLeaveStatusResponseDTO onLeaveStatus = new OnLeaveStatusResponseDTO();
        onLeaveStatus.setOnLeave(false);
        when(leaveServiceClient.isOnLeave(anyLong(), any())).thenReturn(onLeaveStatus);
        when(projectRepository.findByIdAndIsActive(1L, true)).thenReturn(Optional.of(project));
        when(timesheetEntryRepository.existsByUserIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any())).thenReturn(false);
        when(weeklyTimesheetRepository.findByUserIdAndWeekStart(anyLong(), any())).thenReturn(Optional.of(weeklyTimesheet));
        when(timesheetEntryRepository.findByWeeklyTimesheetIdOrderByWorkDateAsc(anyLong())).thenReturn(Collections.singletonList(timesheetEntry));

        TimesheetEntryResponseDTO response = timesheetService.saveEntry(request, userId);

        assertNotNull(response);
        verify(timesheetEntryRepository, times(1)).save(any(TimesheetEntry.class));
        verify(weeklyTimesheetRepository, times(1)).save(weeklyTimesheet);
    }

    @Test
    void saveEntry_FutureDate_ThrowsException() {
        TimesheetEntryCreateDTO request = new TimesheetEntryCreateDTO();
        request.setWorkDate(LocalDate.now().plusDays(1));

        assertThrows(BusinessRuleException.class, () -> timesheetService.saveEntry(request, userId));
    }

    @Test
    void getWeeklyTimesheet_Success() {
        when(weeklyTimesheetRepository.findByUserIdAndWeekStart(userId, monday)).thenReturn(Optional.of(weeklyTimesheet));
        when(timesheetEntryRepository.findByWeeklyTimesheetIdOrderByWorkDateAsc(anyLong())).thenReturn(Collections.singletonList(timesheetEntry));

        WeeklyTimesheetDTO response = timesheetService.getWeeklyTimesheet(monday, userId);

        assertNotNull(response);
        assertEquals(monday, response.getWeekStart());
    }

    @Test
    void deleteEntry_Success() {
        when(timesheetEntryRepository.findById(1L)).thenReturn(Optional.of(timesheetEntry));

        String response = timesheetService.deleteEntry(1L, 1L, null, userId);

        assertEquals("Entry deleted successfully", response);
        verify(timesheetEntryRepository, times(1)).delete(timesheetEntry);
    }
 
    @Test
    void saveEntry_HolidayClash_ThrowsException() {
        TimesheetEntryCreateDTO request = new TimesheetEntryCreateDTO();
        request.setProjectId(1L);
        request.setWorkDate(monday);
        request.setHoursLogged(8.0);
 
        HolidayDTO holiday = new HolidayDTO();
        holiday.setHolidayDate(monday);
        holiday.setName("New Year");
 
        when(leaveServiceClient.getHolidays(anyInt())).thenReturn(Collections.singletonList(holiday));
 
        assertThrows(HolidayClashException.class, () -> timesheetService.saveEntry(request, userId));
    }
 
    @Test
    void saveEntry_OnLeave_ThrowsException() {
        TimesheetEntryCreateDTO request = new TimesheetEntryCreateDTO();
        request.setProjectId(1L);
        request.setWorkDate(monday);
        request.setHoursLogged(8.0);
 
        when(leaveServiceClient.getHolidays(anyInt())).thenReturn(Collections.emptyList());
        OnLeaveStatusResponseDTO onLeaveStatus = new OnLeaveStatusResponseDTO();
        onLeaveStatus.setOnLeave(true); // User is on leave
        when(leaveServiceClient.isOnLeave(anyLong(), any())).thenReturn(onLeaveStatus);
 
        assertThrows(BusinessRuleException.class, () -> timesheetService.saveEntry(request, userId));
    }
 
    @Test
    void saveEntry_Duplicate_ThrowsException() {
        TimesheetEntryCreateDTO request = new TimesheetEntryCreateDTO();
        request.setProjectId(1L);
        request.setWorkDate(monday);
        request.setHoursLogged(8.0);
 
        when(leaveServiceClient.getHolidays(anyInt())).thenReturn(Collections.emptyList());
        OnLeaveStatusResponseDTO onLeaveStatus = new OnLeaveStatusResponseDTO();
        onLeaveStatus.setOnLeave(false);
        when(leaveServiceClient.isOnLeave(anyLong(), any())).thenReturn(onLeaveStatus);
        when(projectRepository.findByIdAndIsActive(1L, true)).thenReturn(Optional.of(project));
        when(timesheetEntryRepository.existsByUserIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any())).thenReturn(true); // Duplicate!
 
        assertThrows(BusinessRuleException.class, () -> timesheetService.saveEntry(request, userId));
    }
 
    @Test
    void saveEntry_InvalidStatus_ThrowsException() {
        TimesheetEntryCreateDTO request = new TimesheetEntryCreateDTO();
        request.setProjectId(1L);
        request.setWorkDate(monday);
        request.setHoursLogged(8.0);
 
        weeklyTimesheet.setStatus(TimesheetStatus.SUBMITTED); // Cannot add to submitted sheet
 
        when(leaveServiceClient.getHolidays(anyInt())).thenReturn(Collections.emptyList());
        OnLeaveStatusResponseDTO onLeaveStatus = new OnLeaveStatusResponseDTO();
        onLeaveStatus.setOnLeave(false);
        when(leaveServiceClient.isOnLeave(anyLong(), any())).thenReturn(onLeaveStatus);
        when(projectRepository.findByIdAndIsActive(1L, true)).thenReturn(Optional.of(project));
        when(timesheetEntryRepository.existsByUserIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any())).thenReturn(false);
        when(weeklyTimesheetRepository.findByUserIdAndWeekStart(anyLong(), any())).thenReturn(Optional.of(weeklyTimesheet));
 
        assertThrows(BusinessRuleException.class, () -> timesheetService.saveEntry(request, userId));
    }

    @Test
    void validateWeek_Valid() {
        when(weeklyTimesheetRepository.findByUserIdAndWeekStart(userId, monday)).thenReturn(Optional.of(weeklyTimesheet));
        
        // Mock 5 entries for Mon-Fri
        List<TimesheetEntry> entries = new java.util.ArrayList<>();
        for(int i=0; i<5; i++) {
            entries.add(TimesheetEntry.builder().workDate(monday.plusDays(i)).hoursLogged(BigDecimal.valueOf(8)).build());
        }
        weeklyTimesheet.setTotalHours(new BigDecimal("40.0"));
        
        when(timesheetEntryRepository.findByWeeklyTimesheetIdOrderByWorkDateAsc(anyLong())).thenReturn(entries);
        when(leaveServiceClient.getHolidays(anyInt())).thenReturn(Collections.emptyList());

        ValidationResultDTO response = timesheetService.validateWeek(monday, userId);

        assertTrue(response.isValid());
        assertTrue(response.getViolations().isEmpty());
    }

    @Test
    void submitWeek_Success() {
        when(weeklyTimesheetRepository.findByUserIdAndWeekStart(userId, monday)).thenReturn(Optional.of(weeklyTimesheet));
        
        // Mock valid week for submission
        List<TimesheetEntry> entries = new java.util.ArrayList<>();
        for(int i=0; i<5; i++) {
            entries.add(TimesheetEntry.builder().workDate(monday.plusDays(i)).hoursLogged(BigDecimal.valueOf(8)).build());
        }
        weeklyTimesheet.setTotalHours(new BigDecimal("40.0"));
        
        when(timesheetEntryRepository.findByWeeklyTimesheetIdOrderByWorkDateAsc(anyLong())).thenReturn(entries);
        when(leaveServiceClient.getHolidays(anyInt())).thenReturn(Collections.emptyList());

        String response = timesheetService.submitWeek(monday, "Submit", userId);

        assertEquals("Timesheet submitted successfully", response);
        assertEquals(TimesheetStatus.SUBMITTED, weeklyTimesheet.getStatus());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(TimesheetSubmittedEvent.class));
    }

    @Test
    void approveTimesheet_Success() {
        weeklyTimesheet.setStatus(TimesheetStatus.SUBMITTED);
        when(weeklyTimesheetRepository.findById(1L)).thenReturn(Optional.of(weeklyTimesheet));

        timesheetService.approveTimesheet(1L, 2L, "Approved");

        assertEquals(TimesheetStatus.APPROVED, weeklyTimesheet.getStatus());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(ApproveCommandEvent.class));
    }

    @Test
    void getActiveProjects_Success() {
        when(projectRepository.findByIsActive(true)).thenReturn(Collections.singletonList(project));

        List<ProjectDTO> response = timesheetService.getActiveProjects();

        assertFalse(response.isEmpty());
        assertEquals("PRJ001", response.get(0).getProjectCode());
    }
 
    @Test
    void rejectTimesheet_Success() {
        weeklyTimesheet.setStatus(TimesheetStatus.SUBMITTED);
        when(weeklyTimesheetRepository.findById(1L)).thenReturn(Optional.of(weeklyTimesheet));
 
        timesheetService.rejectTimesheet(1L, 2L, "Rejected due to incorrect hours");
 
        assertEquals(TimesheetStatus.REJECTED, weeklyTimesheet.getStatus());
        assertEquals("Rejected due to incorrect hours", weeklyTimesheet.getManagerRemark());
    }
 
    @Test
    void validateWeek_BelowMinHours_Invalid() {
        when(weeklyTimesheetRepository.findByUserIdAndWeekStart(userId, monday)).thenReturn(Optional.of(weeklyTimesheet));
        
        // Mock 5 entries but with 4 hours each = 20 hours total (< 40)
        List<TimesheetEntry> entries = new java.util.ArrayList<>();
        for(int i=0; i<5; i++) {
            entries.add(TimesheetEntry.builder().workDate(monday.plusDays(i)).hoursLogged(BigDecimal.valueOf(4)).build());
        }
        weeklyTimesheet.setTotalHours(new BigDecimal("20.0"));
        
        when(timesheetEntryRepository.findByWeeklyTimesheetIdOrderByWorkDateAsc(anyLong())).thenReturn(entries);
        when(leaveServiceClient.getHolidays(anyInt())).thenReturn(Collections.emptyList());
 
        ValidationResultDTO response = timesheetService.validateWeek(monday, userId);
 
        assertFalse(response.isValid());
        assertTrue(response.getViolations().contains("Total hours 20.0 is below minimum required 40"));
    }
 
    @Test
    void getHistory_WithProjectId_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WeeklyTimesheet> page = new PageImpl<>(Collections.singletonList(weeklyTimesheet));
        when(weeklyTimesheetRepository.findByUserIdOrderByWeekStartDesc(userId, pageable)).thenReturn(page);
        when(timesheetEntryRepository.findByWeeklyTimesheetIdOrderByWorkDateAsc(anyLong())).thenReturn(Collections.singletonList(timesheetEntry));
 
        Page<WeeklyTimesheetDTO> response = timesheetService.getHistory(userId, pageable, 1L);
 
        assertFalse(response.isEmpty());
        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getEntries().get(0).getProjectId());
    }
 
    @Test
    void getWeeklyTimesheetById_Success() {
        when(weeklyTimesheetRepository.findById(1L)).thenReturn(Optional.of(weeklyTimesheet));
        when(timesheetEntryRepository.findByWeeklyTimesheetIdOrderByWorkDateAsc(anyLong())).thenReturn(Collections.singletonList(timesheetEntry));
 
        WeeklyTimesheetDTO response = timesheetService.getWeeklyTimesheetById(1L);
 
        assertNotNull(response);
        assertEquals(1L, response.getId());
    }
 
    @Test
    void getWeeklyTimesheetById_NotFound_ThrowsException() {
        when(weeklyTimesheetRepository.findById(99L)).thenReturn(Optional.empty());
 
        assertThrows(ResourceNotFoundException.class, () -> timesheetService.getWeeklyTimesheetById(99L));
    }
 
    @Test
    void deleteEntry_ByDate_Success() {
        when(timesheetEntryRepository.findByUserIdAndProjectIdAndWorkDate(userId, 1L, monday)).thenReturn(Optional.of(timesheetEntry));
 
        String response = timesheetService.deleteEntry(1L, null, monday, userId);
 
        assertEquals("Entry deleted successfully", response);
        verify(timesheetEntryRepository, times(1)).delete(timesheetEntry);
    }
 
    @Test
    void deleteEntry_Unauthorized_ThrowsException() {
        timesheetEntry.setUserId(99L); // Wrong owner
        when(timesheetEntryRepository.findById(1L)).thenReturn(Optional.of(timesheetEntry));
 
        assertThrows(BusinessRuleException.class, () -> timesheetService.deleteEntry(1L, 1L, null, userId));
    }
 
    @Test
    void deleteEntry_MissingParams_ThrowsException() {
        assertThrows(BusinessRuleException.class, () -> timesheetService.deleteEntry(1L, null, null, userId));
    }
}
