package com.cap.leave.repository;

import com.cap.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    // find active leave type by code
    // e.g. findByCodeAndIsActive("CL", true)
    Optional<LeaveType> findByCodeAndIsActive(String code, Boolean isActive);

    // get all active leave types
    // used for dropdown in leave request form
    List<LeaveType> findByIsActive(Boolean isActive);
}