package com.cap.identity.controller;

import com.cap.identity.dto.*;
import com.cap.identity.service.AuthService;
import com.cap.identity.security.InternalAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cap.identity.exception.GlobalExceptionHandler;
import com.cap.identity.security.SecurityConfig;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({ SecurityConfig.class, InternalAuthFilter.class, GlobalExceptionHandler.class })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        // No manual filter mocking needed as we use the real InternalAuthFilter
    }

    @Test
    @WithMockUser
    void signup_Success() throws Exception {
        SignupRequestDTO request = new SignupRequestDTO();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setFullName("Test User");
        request.setEmployeeCode("EMP001");

        when(authService.signup(any())).thenReturn("Registration successful");

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Registration successful"));
    }

    @Test
    void login_Success() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@test.com");
        request.setPassword("password123");

        LoginResponseDTO response = LoginResponseDTO.builder()
                .accessToken("token")
                .userId(1L)
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"));
    }

    @Test
    void forgotPassword_Success() throws Exception {
        ForgotPasswordDTO request = new ForgotPasswordDTO();
        request.setEmail("test@test.com");

        when(authService.forgotPassword(any())).thenReturn(
                ForgotPasswordResponseDTO.builder()
                        .message("If this email exists, a reset token has been sent.")
                        .token("test-token")
                        .build());

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_Success() throws Exception {
        ResetPasswordDTO request = new ResetPasswordDTO();
        request.setToken("valid-token");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        when(authService.resetPassword(any())).thenReturn("Reset successful");

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void getUserById_Self_Success() throws Exception {
        UserProfileDTO profile = UserProfileDTO.builder().id(1L).email("test@test.com").build();
        when(authService.getUserById(1L)).thenReturn(profile);

        mockMvc.perform(get("/auth/users/1")
                .header("X-User-Id", "1")
                .header("X-User-Role", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "2", roles = "EMPLOYEE")
    void getUserById_Other_Forbidden() throws Exception {
        mockMvc.perform(get("/auth/users/1")
                .header("X-User-Id", "2")
                .header("X-User-Role", "EMPLOYEE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "2", roles = "ADMIN")
    void getUserById_Admin_Success() throws Exception {
        UserProfileDTO profile = UserProfileDTO.builder().id(1L).email("test@test.com").build();
        when(authService.getUserById(1L)).thenReturn(profile);

        mockMvc.perform(get("/auth/users/1")
                .header("X-User-Id", "2")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_Success() throws Exception {
        UpdateStatusDTO request = new UpdateStatusDTO();
        request.setStatus("INACTIVE");

        when(authService.updateUserStatus(anyLong(), anyString())).thenReturn("Updated");

        mockMvc.perform(put("/auth/users/1/status")
                .header("X-User-Id", "100")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_Success() throws Exception {
        when(authService.getAllUsers(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/auth/users")
                .header("X-User-Id", "100")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void signup_ValidationError() throws Exception {
        SignupRequestDTO request = new SignupRequestDTO();

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_ValidationError() throws Exception {
        UpdateStatusDTO request = new UpdateStatusDTO();

        mockMvc.perform(put("/auth/users/1/status")
                .header("X-User-Id", "100")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}