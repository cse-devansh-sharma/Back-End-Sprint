package com.cap.notification.service;

import com.cap.notification.dto.NotificationDTO;
import com.cap.notification.entity.Notification;
import com.cap.notification.enums.NotificationType;
import com.cap.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void saveNotification(Long userId, String type, String title, String body) {
        NotificationType notifType;
        try {
            notifType = NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.warn("[NOTIF] Unknown notification type: {}, defaulting to TIMESHEET_SUBMITTED", type);
            notifType = NotificationType.TIMESHEET_SUBMITTED;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(notifType)
                .title(title)
                .body(body)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("[NOTIF] Saved notification type={} for userId={}", type, userId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public NotificationDTO markAsRead(Long notificationId, Long userId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        if (!notif.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: notification does not belong to user");
        }

        notif.setIsRead(true);
        return toDTO(notificationRepository.save(notif));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
