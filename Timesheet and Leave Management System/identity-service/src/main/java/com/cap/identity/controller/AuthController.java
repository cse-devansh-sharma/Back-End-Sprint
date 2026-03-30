package com.cap.identity.controller;

import com.cap.identity.dto.*;
import com.cap.identity.exception.InvalidCredentialsException;
import com.cap.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")  // ← add this
public class AuthController {

    private final AuthService authService;

 
    @Operation(summary = "User Signup", description = "Register a new user in the system")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequestDTO request) {
        return ResponseEntity.status(201).body(authService.signup(request));
    }

 
    @Operation(summary = "User Login", description = "Authenticate a user and return a JWT token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login( @Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Forgot Password", description = "Initiate password reset process")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordDTO request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }


    @Operation(summary = "Reset Password", description = "Complete password reset with new password")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword( @Valid @RequestBody ResetPasswordDTO request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }




    @Operation(summary = "Get User Profile", description = "Retrieve a user profile by ID")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id, Authentication authentication) {

        String currentUserId = authentication.getName();
        String currentRole   = authentication.getAuthorities().iterator().next().getAuthority();

      
        if (!currentRole.equals("ROLE_ADMIN") && !currentUserId.equals(String.valueOf(id))) {
            throw new InvalidCredentialsException("You can only view your own profile");
        }

        return ResponseEntity.ok(authService.getUserById(id));
    }


    @Operation(summary = "Update User Status", description = "Admin updates the status of a user")
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserStatus(@PathVariable Long id,@Valid @RequestBody UpdateStatusDTO request) {
        return ResponseEntity.ok(authService.updateUserStatus(id, request.getStatus()));
    }

    @Operation(summary = "Get All Users", description = "Retrieve a paginated list of users (Admin and HR only)")
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Page<UserProfileDTO>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(authService.getAllUsers(pageable));
    }
}