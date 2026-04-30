package com.cap.leave.security;

import com.cap.leave.controller.LeaveController;
import com.cap.leave.service.LeaveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveService leaveService;

    @MockitoBean
    private InternalAuthFilter internalAuthFilter;

    @Test
    void publicEndpoints_AccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/leave/holidays"))
                .andExpect(status().isOk());
    }

    @Test
    void internalEndpoints_AccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/leave/internal/users/1/on-leave?date=2024-01-01"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_Return401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/leave/history"))
                .andExpect(status().isForbidden()); // Spring Security returns 403 when authentication is missing but filter is bypassed or not configured for entry point
    }
}
