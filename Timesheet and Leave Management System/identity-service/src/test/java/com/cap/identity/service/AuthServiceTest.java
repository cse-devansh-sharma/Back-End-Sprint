package com.cap.identity.service;

import com.cap.identity.client.LeaveServiceClient;
import com.cap.identity.dto.*;
import com.cap.identity.entity.User;
import com.cap.identity.enums.Role;
import com.cap.identity.enums.Status;
import com.cap.identity.exception.InvalidCredentialsException;
import com.cap.identity.exception.UserAlreadyExistsException;
import com.cap.identity.repository.UserRepository;
import com.cap.identity.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private LeaveServiceClient leaveServiceClient;

    @InjectMocks
    private AuthService authService;

    private SignupRequestDTO signupRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequestDTO();
        signupRequest.setEmail("test@test.com");
        signupRequest.setPassword("password");
        signupRequest.setConfirmPassword("password");
        signupRequest.setFullName("Test User");
        signupRequest.setEmployeeCode("EMP001");

        testUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .passwordHash("encodedPassword")
                .role(Role.EMPLOYEE)
                .status(Status.ACTIVE)
                .fullName("Test User")
                .employeeCode("EMP001")
                .failedLoginCount(0)
                .build();
    }

    @Test
    void signup_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmployeeCode(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        String response = authService.signup(signupRequest);

        assertEquals("Registration successful", response);
        verify(userRepository, times(1)).save(any(User.class));
        verify(leaveServiceClient, times(1)).allocateInitialLeaves(any());
    }

    @Test
    void signup_PasswordsDoNotMatch() {
        signupRequest.setConfirmPassword("different");

        assertThrows(InvalidCredentialsException.class, () -> authService.signup(signupRequest));
    }

    @Test
    void signup_EmailAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThrows(UserAlreadyExistsException.class, () -> authService.signup(signupRequest));
    }

    @Test
    void signup_EmployeeCodeAlreadyInUse() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmployeeCode(anyString())).thenReturn(Optional.of(testUser));

        assertThrows(UserAlreadyExistsException.class, () -> authService.signup(signupRequest));
    }

    @Test
    void login_Success() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("fake-token");

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("fake-token", response.getAccessToken());
        assertEquals(1L, response.getUserId());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void login_InvalidCredentials_UserNotFound() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("unknown@test.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_AccountInactive() {
        testUser.setStatus(Status.INACTIVE);
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@test.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_WrongPassword_LocksAccountAfterMaxAttempts() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@test.com");
        request.setPassword("wrong");

        testUser.setFailedLoginCount(4);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        assertEquals(Status.LOCKED, testUser.getStatus());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserProfileDTO response = authService.getUserById(1L);

        assertEquals("test@test.com", response.getEmail());
        assertEquals("EMP001", response.getEmployeeCode());
    }

    @Test
    void updateUserStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        String response = authService.updateUserStatus(1L, "INACTIVE");

        assertEquals("User status updated to INACTIVE", response);
        assertEquals(Status.INACTIVE, testUser.getStatus());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser));

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserProfileDTO> response = authService.getAllUsers(pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("test@test.com", response.getContent().get(0).getEmail());
    }

    @Test
    void forgotPassword_Success() {
        ForgotPasswordDTO request = new ForgotPasswordDTO();
        request.setEmail("test@test.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        String response = authService.forgotPassword(request);

        assertEquals("If this email exists, a reset link has been sent", response);
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordDTO request = new ResetPasswordDTO();
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        String response = authService.resetPassword(request);

        assertEquals("Password reset successful", response);
    }

    @Test
    void resetPassword_PasswordsDoNotMatch() {
        ResetPasswordDTO request = new ResetPasswordDTO();
        request.setNewPassword("newPassword");
        request.setConfirmPassword("different");

        assertThrows(InvalidCredentialsException.class, () -> authService.resetPassword(request));
    }

    @Test
    void login_AccountLocked_ThrowsException() {
        testUser.setStatus(Status.LOCKED);
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@test.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThrows(com.cap.identity.exception.AccountLockedException.class, () -> authService.login(request));
    }

    @Test
    void getUserById_UserNotFound_ThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(com.cap.identity.exception.EntityNotFoundException.class, () -> authService.getUserById(1L));
    }

    @Test
    void updateUserStatus_UserNotFound_ThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(com.cap.identity.exception.EntityNotFoundException.class, () -> authService.updateUserStatus(1L, "ACTIVE"));
    }
}
