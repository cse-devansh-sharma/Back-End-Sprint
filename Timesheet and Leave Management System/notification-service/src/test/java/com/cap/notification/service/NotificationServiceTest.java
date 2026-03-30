package com.cap.notification.service;

import com.cap.notification.dto.NotificationDTO;
import com.cap.notification.entity.Notification;
import com.cap.notification.enums.NotificationType;
import com.cap.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .id(1L)
                .userId(userId)
                .type(NotificationType.LEAVE_APPROVED)
                .title("Leave Approved")
                .body("Your leave has been approved.")
                .isRead(false)
                .build();
    }

    @Test
    void saveNotification_Success() {
        notificationService.saveNotification(userId, "LEAVE_REJECTED", "Rejected", "Reason");

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void saveNotification_InvalidType_DefaultsToTimesheetSubmitted() {
        notificationService.saveNotification(userId, "INVALID_TYPE", "Title", "Body");

        verify(notificationRepository, times(1)).save(argThat(n -> n.getType() == NotificationType.TIMESHEET_SUBMITTED));
    }

    @Test
    void getNotifications_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

        Page<NotificationDTO> response = notificationService.getNotifications(userId, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Leave Approved", response.getContent().get(0).getTitle());
    }

    @Test
    void markAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationDTO response = notificationService.markAsRead(1L, userId);

        assertTrue(response.getIsRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void markAsRead_AccessDenied_ThrowsException() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(1L, 99L));
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countByUserIdAndIsRead(userId, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(userId);

        assertEquals(5, count);
    }
}
