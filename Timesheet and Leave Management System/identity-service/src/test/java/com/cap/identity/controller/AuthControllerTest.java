package com.cap.identity.controller;

import com.cap.identity.dto.*;
import com.cap.identity.exception.InvalidCredentialsException;
import com.cap.identity.service.AuthService;
import com.cap.identity.security.InternalAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private InternalAuthFilter internalAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
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

        when(authService.forgotPassword(any())).thenReturn("Email sent");

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_Success() throws Exception {
        ResetPasswordDTO request = new ResetPasswordDTO();
        request.setNewPassword("newPass");
        request.setConfirmPassword("newPass");

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

        mockMvc.perform(get("/auth/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "2", roles = "EMPLOYEE")
    void getUserById_Other_Forbidden() throws Exception {
        mockMvc.perform(get("/auth/users/1"))
                .andExpect(status().isUnauthorized()); // The controller throws InvalidCredentialsException which might map to 401 or 403 depending on exception handler
    }

    @Test
    @WithMockUser(username = "2", roles = "ADMIN")
    void getUserById_Admin_Success() throws Exception {
        UserProfileDTO profile = UserProfileDTO.builder().id(1L).email("test@test.com").build();
        when(authService.getUserById(1L)).thenReturn(profile);

        mockMvc.perform(get("/auth/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_Success() throws Exception {
        UpdateStatusDTO request = new UpdateStatusDTO();
        request.setStatus("INACTIVE");

        when(authService.updateUserStatus(anyLong(), anyString())).thenReturn("Updated");

        mockMvc.perform(put("/auth/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_Success() throws Exception {
        when(authService.getAllUsers(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/auth/users"))
                .andExpect(status().isOk());
    }

    @Test
    void signup_ValidationError() throws Exception {
        SignupRequestDTO request = new SignupRequestDTO();
        // Missing fields

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_ValidationError() throws Exception {
        UpdateStatusDTO request = new UpdateStatusDTO();
        // Missing status

        mockMvc.perform(put("/auth/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
