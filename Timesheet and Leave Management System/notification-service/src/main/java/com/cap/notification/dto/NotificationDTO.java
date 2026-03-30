package com.cap.notification.dto;

import com.cap.notification.enums.NotificationType;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
	
    private Long  id;
    private NotificationType type;
    private String    title;
    private String    body;
    private Boolean     isRead;
    private LocalDateTime   createdAt;
}
