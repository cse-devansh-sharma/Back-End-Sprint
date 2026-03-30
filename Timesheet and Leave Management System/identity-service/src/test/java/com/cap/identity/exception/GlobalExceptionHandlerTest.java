package com.cap.identity.exception;

import com.cap.identity.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void handleNotFound_Success() {
        EntityNotFoundException ex = new EntityNotFoundException("User not found");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void handleConflict_Success() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("Email exists");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email exists", response.getBody().getMessage());
    }

    @Test
    void handleInvalidCredentials_Success() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Wrong password");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleInvalidCredentials(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Wrong password", response.getBody().getMessage());
    }

    @Test
    void handleLocked_Success() {
        AccountLockedException ex = new AccountLockedException("Locked");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleLocked(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Locked", response.getBody().getMessage());
    }

    @Test
    void handleGeneric_Success() {
        Exception ex = new RuntimeException("Generic error");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGeneric(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Generic error", response.getBody().getMessage());
    }

    @Test
    void handleValidation_Success() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "must not be blank");
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getFieldErrors().containsKey("email"));
        assertEquals("must not be blank", response.getBody().getFieldErrors().get("email"));
    }
}
