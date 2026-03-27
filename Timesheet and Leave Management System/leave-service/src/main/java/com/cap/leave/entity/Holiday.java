package com.cap.leave.entity;

import com.cap.leave.enums.HolidayType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "holidays",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"holiday_date", "type"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HolidayType type; // NATIONAL, OPTIONAL, RESTRICTED

    @Column(nullable = false)
    private Integer year; // derived from holidayDate, stored for fast queries

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}