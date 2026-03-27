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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")  // ← add this
public class AuthController {

    private final AuthService authService;

    // POST /auth/signup
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequestDTO request) {
        return ResponseEntity.status(201)
                .body(authService.signup(request));
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // POST /auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordDTO request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    // POST /auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordDTO request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    // ── PROTECTED ROUTES (JWT required) ──────────────────

    // GET /auth/users/{id}
    // employee can only view their own profile
    // admin can view anyone's profile
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id, Authentication authentication) {

        String currentUserId = authentication.getName();
        String currentRole   = authentication.getAuthorities()
                .iterator().next().getAuthority();

        // if not admin, can only view own profile
        if (!currentRole.equals("ROLE_ADMIN") &&!currentUserId.equals(String.valueOf(id))) {
            throw new InvalidCredentialsException("You can only view your own profile");
        }

        return ResponseEntity.ok(authService.getUserById(id));
    }

    // PUT /auth/users/{id}/status
    // ADMIN only
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserStatus(@PathVariable Long id,@Valid @RequestBody UpdateStatusDTO request) {
        return ResponseEntity.ok(authService.updateUserStatus(id, request.getStatus()));
    }

    // GET /auth/users
    // ADMIN and HR only — paginated list
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Page<UserProfileDTO>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(authService.getAllUsers(pageable));
    }
}