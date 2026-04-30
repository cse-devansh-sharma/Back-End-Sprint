package com.cap.identity.service;

import com.cap.identity.dto.*;
import com.cap.identity.entity.PasswordResetToken;
import com.cap.identity.entity.User;
import com.cap.identity.enums.Role;
import com.cap.identity.enums.Status;
import com.cap.identity.exception.*;
import com.cap.identity.repository.PasswordResetTokenRepository;
import com.cap.identity.repository.UserRepository;
import com.cap.identity.util.JwtUtil;
import com.cap.identity.client.LeaveServiceClient;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LeaveServiceClient leaveServiceClient;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    public String signup(SignupRequestDTO request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        if (userRepository.findByEmployeeCode(request.getEmployeeCode()).isPresent()) {
            throw new UserAlreadyExistsException("Employee code already in use");
        }

        User user = User.builder()
                .employeeCode(request.getEmployeeCode())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.EMPLOYEE)
                .status(Status.ACTIVE)
                .failedLoginCount(0)
                .isDeleted(false)
                .createdBy(request.getEmail())
                .build();

        userRepository.save(user);

        try {
            leaveServiceClient.allocateInitialLeaves(user.getId());
        } catch (Exception e) {
       
            System.err.println("Leave allocation failed: " + e.getMessage());
        }

        return "Registration successful";
    }

    public LoginResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.getStatus() == Status.INACTIVE) {
            throw new InvalidCredentialsException("Account is inactive");
        }

        if (user.getStatus() == Status.LOCKED) {
            throw new AccountLockedException("Account locked. Contact admin.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginCount(user.getFailedLoginCount() + 1);
            if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(Status.LOCKED);
            }
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        user.setFailedLoginCount(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        return LoginResponseDTO.builder()
                .accessToken(token)
                .role(user.getRole().name())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .expiresIn(3600000)
                .build();
    }

    public ForgotPasswordResponseDTO forgotPassword(ForgotPasswordDTO request) {
        return userRepository.findByEmail(request.getEmail())
                .map(user -> {
                    // invalidate any existing unused tokens for this user
                    passwordResetTokenRepository.findAll().stream()
                            .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isUsed())
                            .forEach(t -> { t.setUsed(true); passwordResetTokenRepository.save(t); });

                    // generate new token — expires in 15 minutes
                    String rawToken = UUID.randomUUID().toString().replace("-", "");
                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            .user(user)
                            .token(rawToken)
                            .expiryDate(LocalDateTime.now().plusMinutes(15))
                            .used(false)
                            .build();
                    passwordResetTokenRepository.save(resetToken);

                    // send via email service (dev: returns token, prod: sends email)
                    String returnedToken = emailService.sendPasswordResetToken(user.getEmail(), rawToken);

                    return ForgotPasswordResponseDTO.builder()
                            .message("If this email exists, a reset token has been sent.")
                            .token(returnedToken) // dev mode only
                            .build();
                })
                .orElse(ForgotPasswordResponseDTO.builder()
                        .message("If this email exists, a reset token has been sent.")
                        .token(null)
                        .build());
    }

    public String resetPassword(ResetPasswordDTO request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedFalseAndExpiryDateAfter(request.getToken(), LocalDateTime.now());

        if (resetToken == null) {
            throw new InvalidCredentialsException("Token is invalid or has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginCount(0);
        if (user.getStatus() == Status.LOCKED) user.setStatus(Status.ACTIVE);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return "Password reset successful";
    }

    public UserProfileDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        return UserProfileDTO.builder()
                .id(user.getId())
                .employeeCode(user.getEmployeeCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();
    }

    public String updateUserStatus(Long id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        user.setStatus(Status.valueOf(status.toUpperCase()));
        userRepository.save(user);
        return "User status updated to " + status;
    }

    public Page<UserProfileDTO> getAllUsers(Pageable pageable) {

        return userRepository.findAll(pageable)
                .map(user -> UserProfileDTO.builder()
                        .id(user.getId())
                        .employeeCode(user.getEmployeeCode())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .status(user.getStatus().name())
                        .build());
    }
}