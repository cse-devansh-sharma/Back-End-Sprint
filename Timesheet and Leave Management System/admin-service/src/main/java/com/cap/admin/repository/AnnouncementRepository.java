package com.cap.admin.repository;

import com.cap.admin.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

    // get active announcements that haven't expired
    List<Announcement> findByIsActiveAndExpiresAtAfter(
            Boolean isActive,
            LocalDateTime now);
}