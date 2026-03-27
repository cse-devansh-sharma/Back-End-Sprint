package com.cap.timesheet.exception;

import com.cap.timesheet.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HolidayClashException.class)
    public ResponseEntity<ErrorResponse> handleHolidayClashException(HolidayClashException ex, HttpServletRequest request) {
        log.error("Holiday Clash Violation: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(BusinessRuleException ex, HttpServletRequest request) {
        log.error("Business Rule Violation: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource Not Found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access Denied: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected Error occurs: ", ex);
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, String path) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(path)
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }
}
