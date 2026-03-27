package com.cap.timesheet.repository;

import com.cap.timesheet.entity.TimesheetAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimesheetAuditLogRepository
        extends JpaRepository<TimesheetAuditLog, Long> {

    // full audit trail for a timesheet ordered by time
	List<TimesheetAuditLog> findByTimesheetIdOrderByPerformedAtAsc(
	        Long timesheetId);
}