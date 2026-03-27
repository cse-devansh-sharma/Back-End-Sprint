package com.cap.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "leave_policies",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"leave_type_code", "year"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeavePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_code",
            nullable = false, length = 30)
    private String policyCode;

    // logical key — no FK to leave_db
    @Column(name = "leave_type_code",
            nullable = false, length = 20)
    private String leaveTypeCode;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "allotment_days",
            nullable = false, precision = 4, scale = 1)
    private BigDecimal allotmentDays;

    @Column(name = "carry_forward_days",
            precision = 4, scale = 1)
    private BigDecimal carryForwardDays = BigDecimal.ZERO;

    @Column(name = "encashment_allowed")
    private Boolean encashmentAllowed = false;

    @Column(name = "probation_exclusion")
    private Boolean probationExclusion = false;

    @Column(name = "is_active")
    private Boolean isActive = true;
}