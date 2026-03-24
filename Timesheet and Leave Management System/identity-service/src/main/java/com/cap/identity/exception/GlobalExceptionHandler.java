package com.cap.identity.exception;

import com.cap.identity.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 — resource not found ─────────────────────────
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        return build(HttpStatus.NOT_FOUND,
                "Not Found", ex.getMessage(), request);
    }

    // ── 409 — email/code already exists ─────────────────
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        return build(HttpStatus.CONFLICT,
                "Conflict", ex.getMessage(), request);
    }

    // ── 401 — wrong credentials ──────────────────────────
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        return build(HttpStatus.UNAUTHORIZED,
                "Unauthorized", ex.getMessage(), request);
    }

    // ── 403 — account locked ─────────────────────────────
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponseDTO> handleLocked(
            AccountLockedException ex,
            HttpServletRequest request) {

        return build(HttpStatus.FORBIDDEN,
                "Forbidden", ex.getMessage(), request);
    }

    // ── 400 — validation errors (@Valid failed) ──────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // collect all field errors into a map
        // e.g. { "email": "must not be blank", "password": "too short" }
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponseDTO response = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // ── 500 — anything else ──────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", ex.getMessage(), request);
    }

    // ── helper to build consistent response ─────────────
    private ResponseEntity<ErrorResponseDTO> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request) {

        ErrorResponseDTO response = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(null)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}