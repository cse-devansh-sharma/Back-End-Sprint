package com.cap.admin.repository;

import com.cap.admin.entity.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeavePolicyRepository
        extends JpaRepository<LeavePolicy, Long> {

    Optional<LeavePolicy> findByLeaveTypeCodeAndYear(
            String leaveTypeCode,
            Integer year);

    List<LeavePolicy> findByYearAndIsActive(
            Integer year,
            Boolean isActive);

    boolean existsByLeaveTypeCodeAndYear(String leaveTypeCode, Integer year);
}