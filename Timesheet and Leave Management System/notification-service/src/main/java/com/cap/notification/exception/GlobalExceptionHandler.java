package com.cap.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("[NOTIF] Unhandled exception: {}", ex.getMessage());
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (msg != null && msg.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (msg != null && (msg.contains("Access denied") || msg.contains("does not belong"))) {
            status = HttpStatus.FORBIDDEN;
        }

        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status",    status.value(),
                "error",     status.getReasonPhrase(),
                "message",   msg != null ? msg : "An unexpected error occurred"
        ));
    }
}
