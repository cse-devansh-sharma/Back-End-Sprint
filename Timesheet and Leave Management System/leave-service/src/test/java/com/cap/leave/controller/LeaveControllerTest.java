package com.cap.leave.controller;

import com.cap.leave.dto.*;
import com.cap.leave.enums.LeaveStatus;
import com.cap.leave.security.InternalAuthFilter;
import com.cap.leave.service.LeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to focus on controller logic
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveService leaveService;

    @MockitoBean
    private InternalAuthFilter internalAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void createLeaveRequest_Success() throws Exception {
        LeaveRequestCreateDTO request = new LeaveRequestCreateDTO();
        request.setLeaveTypeId(1L);
        request.setFromDate(LocalDate.now().plusDays(1));
        request.setToDate(LocalDate.now().plusDays(2));
        request.setReason("Test");

        LeaveRequestResponseDTO response = LeaveRequestResponseDTO.builder()
                .id(1L)
                .status(LeaveStatus.SUBMITTED)
                .build();

        when(leaveService.createLeaveRequest(any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/leave/requests")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "1")
    void getLeaveStatusById_Success() throws Exception {
        LeaveStatusResponseDTO response = new LeaveStatusResponseDTO(1L, LeaveStatus.APPROVED);
        when(leaveService.getLeaveStatusById(anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/leave/requests/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "1")
    void getLeaveHistory_Success() throws Exception {
        when(leaveService.getLeaveHistory(anyLong(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/leave/history"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1")
    void cancelLeave_Success() throws Exception {
        when(leaveService.cancelLeave(anyLong(), anyLong())).thenReturn("Cancelled");

        mockMvc.perform(delete("/leave/requests/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Cancelled"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getBalances_Success() throws Exception {
        when(leaveService.getLeaveBalances(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/leave/balance/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getHolidays_Success() throws Exception {
        when(leaveService.getHolidaysByYear(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/leave/holidays?year=2026"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void isOnLeave_Success() throws Exception {
        when(leaveService.isOnLeave(anyLong(), any())).thenReturn(true);

        mockMvc.perform(get("/leave/users/1/on-leave?date=2026-04-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onLeave").value(true));
    }

    @Test
    void allocateInitialLeaves_Success() throws Exception {
        mockMvc.perform(post("/leave/internal/users/1/allocate-initial")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void manualAllocate_Success() throws Exception {
        LeaveAllocationRequestDTO request = new LeaveAllocationRequestDTO();
        request.setUserId(1L);
        request.setLeaveTypeCode("CL");
        request.setAmount(java.math.BigDecimal.TEN);

        mockMvc.perform(post("/leave/allocate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
