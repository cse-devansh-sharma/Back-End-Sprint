package com.cap.timesheet.controller;

import com.cap.timesheet.dto.*;
import com.cap.timesheet.service.TimesheetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimesheetController.class)
class TimesheetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimesheetService timesheetService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void saveEntry_Success() throws Exception {
        TimesheetEntryCreateDTO request = new TimesheetEntryCreateDTO();
        request.setProjectId(1L);
        request.setWorkDate(LocalDate.now());
        request.setHoursLogged(8.0);

        TimesheetEntryResponseDTO response = TimesheetEntryResponseDTO.builder()
                .id(1L)
                .projectId(1L)
                .build();

        when(timesheetService.saveEntry(any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/timesheet/entries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void getWeeklyTimesheet_Success() throws Exception {
        LocalDate monday = LocalDate.of(2026, 3, 30);
        WeeklyTimesheetDTO response = WeeklyTimesheetDTO.builder()
                .id(1L)
                .weekStart(monday)
                .build();

        when(timesheetService.getWeeklyTimesheet(any(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/timesheet/weeks/2026-03-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void submitWeek_Success() throws Exception {
        SubmitWeekDTO request = new SubmitWeekDTO();
        request.setComment("Weekly submission");

        when(timesheetService.submitWeek(any(), any(), anyLong())).thenReturn("Submitted");

        mockMvc.perform(post("/timesheet/weeks/2026-03-30/submit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Submitted"));
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void getProjects_Success() throws Exception {
        ProjectDTO project = ProjectDTO.builder().id(1L).projectCode("P1").build();
        when(timesheetService.getActiveProjects()).thenReturn(Collections.singletonList(project));

        mockMvc.perform(get("/timesheet/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectCode").value("P1"));
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void deleteEntry_Success() throws Exception {
        when(timesheetService.deleteEntry(anyLong(), anyLong(), any(), anyLong())).thenReturn("Deleted");

        mockMvc.perform(delete("/timesheet/projects/1/entries/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted"));
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void validateWeek_Success() throws Exception {
        ValidationResultDTO response = ValidationResultDTO.builder().isValid(true).build();
        when(timesheetService.validateWeek(any(), anyLong())).thenReturn(response);

        mockMvc.perform(get("/timesheet/weeks/2026-03-30/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void getHistory_Success() throws Exception {
        when(timesheetService.getHistory(anyLong(), any(), anyLong())).thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/timesheet/history")
                .param("projectId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getWeeklyTimesheetById_Success() throws Exception {
        WeeklyTimesheetDTO response = WeeklyTimesheetDTO.builder().id(1L).build();
        when(timesheetService.getWeeklyTimesheetById(anyLong())).thenReturn(response);

        mockMvc.perform(get("/timesheet/internal/weeks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
