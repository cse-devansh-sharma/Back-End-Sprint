package com.cap.timesheet.repository;

import com.cap.timesheet.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    // find active project by id
    Optional<Project> findByIdAndIsActive(
            Long id,
            Boolean isActive);

    // get all active projects for dropdown
    List<Project> findByIsActive(Boolean isActive);

    // find by project code
    Optional<Project> findByProjectCodeAndIsActive(
            String projectCode,
            Boolean isActive);

    // find by project code (for sync upsert)
    Optional<Project> findByProjectCode(String projectCode);
}