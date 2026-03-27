package com.cap.admin.messaging.dto;

import lombok.*;

/**
 * Published by admin/leave/timesheet service to notification exchange.
 * Consumed by notification-service to persist and serve notifications.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEvent {
    private Long   userId;
    private String type;    // e.g. "LEAVE_APPROVED", "TIMESHEET_SUBMITTED"
    private String title;
    private String body;
}
