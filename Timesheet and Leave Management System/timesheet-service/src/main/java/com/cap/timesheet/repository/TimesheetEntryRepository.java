package com.cap.timesheet.repository;

import com.cap.timesheet.entity.TimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetEntryRepository
        extends JpaRepository<TimesheetEntry, Long> {

    // get all entries for a weekly timesheet ordered by date
    List<TimesheetEntry> findByWeeklyTimesheetIdOrderByWorkDateAsc(
            Long weeklyTimesheetId);

    // check duplicate — same user + project + date
    boolean existsByUserIdAndProjectIdAndWorkDate(
            Long userId,
            Long projectId,
            LocalDate workDate);

    // get all entries for a user on a specific date
    List<TimesheetEntry> findByUserIdAndWorkDate(
            Long userId,
            LocalDate workDate);

    // find a specific entry by user, project and date
    java.util.Optional<TimesheetEntry> findByUserIdAndProjectIdAndWorkDate(
            Long userId,
            Long projectId,
            LocalDate workDate);
}