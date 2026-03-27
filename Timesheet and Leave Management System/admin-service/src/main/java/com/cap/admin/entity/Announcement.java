package com.cap.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}