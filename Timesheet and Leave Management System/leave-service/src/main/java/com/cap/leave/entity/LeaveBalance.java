package com.cap.leave.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "leave_balances",
    uniqueConstraints = {
        // one balance record per user per leave type per year
        @UniqueConstraint(
            columnNames = {"user_id", "leave_type_id", "year"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId from JWT — no FK to auth_db
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // FK within same schema — leave_types.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "total_allotted", nullable = false,
            precision = 4, scale = 1)
    private BigDecimal totalAllotted;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal used = BigDecimal.ZERO;

    // days reserved while request is SUBMITTED but not yet approved
    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal pending = BigDecimal.ZERO;

    @Column(name = "carry_forwarded", nullable = false,
            precision = 4, scale = 1)
    private BigDecimal carryForwarded = BigDecimal.ZERO;
}