package com.cap.leave.repository;

import com.cap.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    // find specific balance for one user + type + year
    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId, Integer year);

    // get all balances for a user in a specific year
    // used for dashboard balance cards
    List<LeaveBalance> findByUserIdAndYear(Long userId, Integer year);
}