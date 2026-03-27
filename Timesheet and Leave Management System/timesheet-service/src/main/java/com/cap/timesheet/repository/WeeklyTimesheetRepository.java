package com.cap.timesheet.repository;

import com.cap.timesheet.entity.WeeklyTimesheet;
import com.cap.timesheet.enums.TimesheetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyTimesheetRepository
        extends JpaRepository<WeeklyTimesheet, Long> {

    // find specific week for a user
    Optional<WeeklyTimesheet> findByUserIdAndWeekStart(
            Long userId,
            LocalDate weekStart);

    // find all timesheets for a user by status
    List<WeeklyTimesheet> findByUserIdAndStatus(
            Long userId,
            TimesheetStatus status);

    // paginated history for employee
    Page<WeeklyTimesheet> findByUserIdOrderByWeekStartDesc(
            Long userId,
            Pageable pageable);

    // find all submitted timesheets for a manager's team
    List<WeeklyTimesheet> findByUserIdInAndStatus(
            List<Long> userIds,
            TimesheetStatus status);
}