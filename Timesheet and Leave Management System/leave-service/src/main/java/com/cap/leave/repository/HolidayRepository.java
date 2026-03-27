package com.cap.leave.repository;

import com.cap.leave.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    // get all active holidays for a given year
    // used when building leave calendar
    List<Holiday> findByYearAndIsActive(Integer year,Boolean isActive);

    // get holidays between two dates
    // used by timesheet service validation
    // and numberOfDays calculation
    List<Holiday> findByHolidayDateBetweenAndIsActive(LocalDate startDate, LocalDate endDate, Boolean isActive);
}