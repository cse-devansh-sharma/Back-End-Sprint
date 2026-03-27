package com.cap.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementDTO {

    private Long          id;
    private String        title;
    private String        body;
    private Boolean       isActive;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
}