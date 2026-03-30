package com.cap.leave.service;

import com.cap.leave.dto.*;
import com.cap.leave.entity.*;
import com.cap.leave.enums.LeaveStatus;
import com.cap.leave.exception.BusinessRuleException;
import com.cap.leave.exception.LeaveNotFoundException;
import com.cap.leave.exception.ResourceNotFoundException;
import com.cap.leave.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;
    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private LeaveAuditLogRepository leaveAuditLogRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LeaveService leaveService;

    private LeaveType leaveType;
    private LeaveBalance leaveBalance;
    private LeaveRequest leaveRequest;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(leaveService, "cancellationDaysBefore", 1);

        leaveType = LeaveType.builder()
                .id(1L)
                .code("CL")
                .name("Casual Leave")
                .isActive(true)
                .minNoticeDays(0)
                .requiresDelegate(false)
                .build();

        leaveBalance = LeaveBalance.builder()
                .id(1L)
                .userId(userId)
                .leaveType(leaveType)
                .year(LocalDate.now().getYear())
                .totalAllotted(new BigDecimal("12.00"))
                .used(BigDecimal.ZERO)
                .pending(BigDecimal.ZERO)
                .build();

        LocalDate start = LocalDate.of(2026, 4, 6); // Monday
        LocalDate end = LocalDate.of(2026, 4, 7);   // Tuesday

        leaveRequest = LeaveRequest.builder()
                .id(1L)
                .userId(userId)
                .leaveType(leaveType)
                .fromDate(start)
                .toDate(end)
                .numberOfDays(new BigDecimal("2.00"))
                .status(LeaveStatus.SUBMITTED)
                .build();
    }

    @Test
    void createLeaveRequest_Success() {
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setFromDate(LocalDate.of(2026, 4, 6));
        requestDTO.setToDate(LocalDate.of(2026, 4, 7));
        requestDTO.setReason("Vacation");

        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.findOverlappingLeave(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(holidayRepository.findByHolidayDateBetweenAndIsActive(any(), any(), anyBoolean())).thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.of(leaveBalance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        LeaveRequestResponseDTO response = leaveService.createLeaveRequest(requestDTO, userId);

        assertNotNull(response);
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
       // verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    void createLeaveRequest_InsufficientBalance() {
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setFromDate(LocalDate.of(2026, 4, 6));
        requestDTO.setToDate(LocalDate.of(2026, 4, 7));

        leaveBalance.setTotalAllotted(new BigDecimal("1.00"));

        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.of(leaveBalance));

        assertThrows(BusinessRuleException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void createLeaveRequest_LeaveTypeNotFound() {
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(99L);
        when(leaveTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void createLeaveRequest_InactiveLeaveType() {
        leaveType.setIsActive(false);
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        assertThrows(BusinessRuleException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void createLeaveRequest_FromDateAfterToDate() {
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setFromDate(LocalDate.of(2026, 4, 10));
        requestDTO.setToDate(LocalDate.of(2026, 4, 5));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        assertThrows(BusinessRuleException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void createLeaveRequest_MinNoticeDaysViolation() {
        leaveType.setMinNoticeDays(10);
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setFromDate(LocalDate.now().plusDays(5));
        requestDTO.setToDate(LocalDate.now().plusDays(6));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        assertThrows(BusinessRuleException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void createLeaveRequest_DelegateRequiredMissing() {
        leaveType.setRequiresDelegate(true);
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setFromDate(LocalDate.of(2026, 4, 6));
        requestDTO.setToDate(LocalDate.of(2026, 4, 7));
        requestDTO.setDelegateUserId(null);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        assertThrows(BusinessRuleException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void createLeaveRequest_OverlappingLeave() {
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setFromDate(LocalDate.of(2026, 4, 6));
        requestDTO.setToDate(LocalDate.of(2026, 4, 7));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.findOverlappingLeave(any(), any(), any(), any())).thenReturn(List.of(leaveRequest));

        assertThrows(BusinessRuleException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void createLeaveRequest_NoWorkingDays() {
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setFromDate(LocalDate.of(2026, 4, 4)); // Saturday
        requestDTO.setToDate(LocalDate.of(2026, 4, 5));   // Sunday
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.findOverlappingLeave(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        assertThrows(BusinessRuleException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void createLeaveRequest_BalanceNotFound() {
        LeaveRequestCreateDTO requestDTO = new LeaveRequestCreateDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setFromDate(LocalDate.of(2026, 4, 6));
        requestDTO.setToDate(LocalDate.of(2026, 4, 7));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.findOverlappingLeave(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(holidayRepository.findByHolidayDateBetweenAndIsActive(any(), any(), anyBoolean())).thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.createLeaveRequest(requestDTO, userId));
    }

    @Test
    void approveLeave_Success() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.of(leaveBalance));

        leaveService.approveLeave(1L, 2L, "Approved");

        assertEquals(LeaveStatus.APPROVED, leaveRequest.getStatus());
        verify(leaveRequestRepository, times(1)).save(leaveRequest);
        verify(leaveBalanceRepository, times(1)).save(leaveBalance);
    }

    @Test
    void approveLeave_NotFound() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(LeaveNotFoundException.class, () -> leaveService.approveLeave(99L, 2L, "Approved"));
    }

    @Test
    void approveLeave_InvalidStatus() {
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        assertThrows(BusinessRuleException.class, () -> leaveService.approveLeave(1L, 2L, "Approved"));
    }

    @Test
    void rejectLeave_Success() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.of(leaveBalance));

        leaveService.rejectLeave(1L, 2L, "Rejected");

        assertEquals(LeaveStatus.REJECTED, leaveRequest.getStatus());
        verify(leaveRequestRepository, times(1)).save(leaveRequest);
    }

    @Test
    void rejectLeave_NotFound() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(LeaveNotFoundException.class, () -> leaveService.rejectLeave(99L, 2L, "Rejected"));
    }

    @Test
    void cancelLeave_Success_FromSubmitted() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.of(leaveBalance));

        String response = leaveService.cancelLeave(1L, userId);

        assertEquals("Leave request cancelled successfully", response);
        assertEquals(LeaveStatus.CANCELLED, leaveRequest.getStatus());
        verify(leaveRequestRepository, times(1)).save(leaveRequest);
        verify(leaveBalanceRepository, times(1)).save(leaveBalance);
    }

    @Test
    void cancelLeave_Success_FromApproved() {
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.of(leaveBalance));

        String response = leaveService.cancelLeave(1L, userId);

        assertEquals("Leave request cancelled successfully", response);
        assertEquals(LeaveStatus.CANCELLED, leaveRequest.getStatus());
        verify(leaveBalanceRepository, times(1)).save(leaveBalance);
    }

    @Test
    void cancelLeave_Unauthorized() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        assertThrows(BusinessRuleException.class, () -> leaveService.cancelLeave(1L, 99L));
    }

    @Test
    void cancelLeave_InvalidStatus() {
        leaveRequest.setStatus(LeaveStatus.REJECTED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        assertThrows(BusinessRuleException.class, () -> leaveService.cancelLeave(1L, userId));
    }

    @Test
    void cancelLeave_WindowViolation() {
        leaveRequest.setFromDate(LocalDate.now()); // starts today
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        assertThrows(BusinessRuleException.class, () -> leaveService.cancelLeave(1L, userId));
    }

    @Test
    void getLeaveHistory_Success() {
        Page<LeaveRequest> page = new PageImpl<>(List.of(leaveRequest));
        when(leaveRequestRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(page);

        Page<LeaveRequestResponseDTO> response = leaveService.getLeaveHistory(userId, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getLeaveBalances_Success() {
        when(leaveBalanceRepository.findByUserIdAndYear(anyLong(), anyInt())).thenReturn(Collections.singletonList(leaveBalance));

        List<LeaveBalanceDTO> response = leaveService.getLeaveBalances(userId);

        assertFalse(response.isEmpty());
        assertEquals("CL", response.get(0).getLeaveTypeCode());
    }

    @Test
    void getLeaveRequestById_Success() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        LeaveRequestResponseDTO response = leaveService.getLeaveRequestById(1L, userId);
        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getLeaveRequestById_NotFound() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(LeaveNotFoundException.class, () -> leaveService.getLeaveRequestById(99L, userId));
    }

    @Test
    void getLeaveStatusById_Success() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        LeaveStatusResponseDTO response = leaveService.getLeaveStatusById(1L, userId);
        assertNotNull(response);
        assertEquals(LeaveStatus.SUBMITTED, response.getStatus());
    }

    @Test
    void getTeamCalendar_Success() {
        when(leaveRequestRepository.findByUserIdInAndStatus(anyList(), any())).thenReturn(List.of(leaveRequest));
        List<TeamCalendarDTO> response = leaveService.getTeamCalendar(List.of(userId), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void getHolidaysByYear_Success() {
        Holiday holiday = Holiday.builder().id(1L).name("Cristmas").holidayDate(LocalDate.of(2026, 12, 25)).year(2026).isActive(true).build();
        when(holidayRepository.findByYearAndIsActive(2026, true)).thenReturn(List.of(holiday));
        List<HolidayDTO> response = leaveService.getHolidaysByYear(2026);
        assertEquals(1, response.size());
    }

    @Test
    void isOnLeave_True() {
        when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any(), any())).thenReturn(Collections.singletonList(leaveRequest));

        boolean response = leaveService.isOnLeave(userId, LocalDate.now());

        assertTrue(response);
    }

    @Test
    void isOnLeave_False() {
        when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any(), any())).thenReturn(Collections.emptyList());
        boolean response = leaveService.isOnLeave(userId, LocalDate.now());
        assertFalse(response);
    }

    @Test
    void allocateInitialLeaves_Success() {
        when(leaveTypeRepository.findByIsActive(true)).thenReturn(List.of(leaveType, 
            LeaveType.builder().code("SL").isActive(true).build()));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), any(), anyInt())).thenReturn(Optional.empty());

        leaveService.allocateInitialLeaves(userId);

        verify(leaveBalanceRepository, atLeastOnce()).save(any(LeaveBalance.class));
    }

    @Test
    void manualAllocateLeaves_Success_ExistingBalance() {
        LeaveAllocationRequestDTO request = new LeaveAllocationRequestDTO();
        request.setUserId(userId);
        request.setLeaveTypeCode("CL");
        request.setAmount(new BigDecimal("5.00"));

        when(leaveTypeRepository.findByCodeAndIsActive(anyString(), anyBoolean())).thenReturn(Optional.of(leaveType));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.of(leaveBalance));

        leaveService.manualAllocateLeaves(request);

        assertTrue(new BigDecimal("17.00").compareTo(leaveBalance.getTotalAllotted()) == 0);
        verify(leaveBalanceRepository, times(1)).save(leaveBalance);
    }

    @Test
    void manualAllocateLeaves_Success_NewBalance() {
        LeaveAllocationRequestDTO request = new LeaveAllocationRequestDTO();
        request.setUserId(userId);
        request.setLeaveTypeCode("CL");
        request.setAmount(new BigDecimal("5.00"));

        when(leaveTypeRepository.findByCodeAndIsActive(anyString(), anyBoolean())).thenReturn(Optional.of(leaveType));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(anyLong(), anyLong(), anyInt())).thenReturn(Optional.empty());

        leaveService.manualAllocateLeaves(request);

        verify(leaveBalanceRepository, times(1)).save(any(LeaveBalance.class));
    }

    @Test
    void manualAllocateLeaves_TypeNotFound() {
        LeaveAllocationRequestDTO request = new LeaveAllocationRequestDTO();
        request.setLeaveTypeCode("XX");
        when(leaveTypeRepository.findByCodeAndIsActive("XX", true)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.manualAllocateLeaves(request));
    }
}
