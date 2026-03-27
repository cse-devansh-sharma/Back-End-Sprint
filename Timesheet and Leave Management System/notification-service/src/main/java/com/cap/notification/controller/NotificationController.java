package com.cap.notification.controller;

import com.cap.notification.dto.NotificationDTO;
import com.cap.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get My Notifications",
            description = "Returns a paginated list of notifications for the logged-in user, ordered by most recent first.")
    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getMyNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(notificationService.getNotifications(userId, pageable));
    }

    @Operation(summary = "Get Unread Count",
            description = "Returns the number of unread notifications for the logged-in user. Use this to show badge counts in the UI.")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "Mark Notification as Read",
            description = "Marks a specific notification as read. Only the owner of the notification can mark it.")
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }
}
