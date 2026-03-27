package com.cap.leave.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CL, SL, EL, COMP_OFF, OPTIONAL
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = true;

    // if true employee must name a delegate when applying
    @Column(name = "requires_delegate", nullable = false)
    private Boolean requiresDelegate = false;

    // how many days before leave must be applied
    @Column(name = "min_notice_days", nullable = false)
    private Integer minNoticeDays = 0;

    @Column(name = "allow_half_day", nullable = false)
    private Boolean allowHalfDay = false;

    @Column(name = "carry_forward_allowed", nullable = false)
    private Boolean carryForwardAllowed = false;

    @Column(name = "max_carry_forward_days", nullable = false)
    private Integer maxCarryForwardDays = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}