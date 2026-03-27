package com.cap.timesheet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    private Long id;

    @Column(name = "project_code", nullable = false, unique = true)
    private String projectCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "cost_center")
    private String costCenter;

    @Column(name = "is_billable")
    private Boolean isBillable;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;
}