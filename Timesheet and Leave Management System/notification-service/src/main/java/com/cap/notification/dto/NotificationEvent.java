package com.cap.notification.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEvent {
    private Long   userId;
    private String type;   // matches NotificationType enum name
    private String title;
    private String body;
}
