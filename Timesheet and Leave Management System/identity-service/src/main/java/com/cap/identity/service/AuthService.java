package com.cap.identity.service;

import com.cap.identity.dto.*;
import com.cap.identity.entity.User;
import com.cap.identity.enums.Role;
import com.cap.identity.enums.Status;
import com.cap.identity.exception.*;
import com.cap.identity.repository.UserRepository;
import com.cap.identity.util.JwtUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    private static final int MAX_FAILED_ATTEMPTS = 5;

   
    public String signup(SignupRequestDTO request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        if (userRepository.findByEmployeeCode(request.getEmployeeCode()).isPresent()) {
            throw new EmailAlreadyExistsException("Employee code already in use");
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
        return "Registration successful";
    }

    public LoginResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid credentials"));

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


    public String forgotPassword(ForgotPasswordDTO request) {

        
        
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            System.out.println("Reset token generated for: " + user.getEmail());
        });

        return "If this email exists, a reset link has been sent";
    }

    
    public String resetPassword(ResetPasswordDTO request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        
        return "Password reset successful";
    }

 
    public UserProfileDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with id: " + id));

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
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with id: " + id));

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