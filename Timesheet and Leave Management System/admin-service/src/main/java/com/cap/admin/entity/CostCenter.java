package com.cap.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cost_centers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_active")
    private Boolean isActive = true;
}