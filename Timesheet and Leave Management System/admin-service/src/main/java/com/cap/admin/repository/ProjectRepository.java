package com.cap.admin.repository;

import com.cap.admin.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    Optional<Project> findByProjectCodeAndIsActive(
            String projectCode,
            Boolean isActive);

    List<Project> findByIsActive(Boolean isActive);

    boolean existsByProjectCode(String projectCode);
}