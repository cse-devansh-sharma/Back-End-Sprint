package com.cap.leave.repository;

import com.cap.leave.entity.LeaveAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveAuditLogRepository extends JpaRepository<LeaveAuditLog, Long> {

    // get full audit trail for a leave request
    List<LeaveAuditLog> findByLeaveRequestIdOrderByPerformedAtAsc( Long requestId);
}