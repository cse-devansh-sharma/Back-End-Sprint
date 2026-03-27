package com.cap.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code",
            nullable = false, unique = true, length = 30)
    private String projectCode;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "is_billable")
    private Boolean isBillable = true;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;
}