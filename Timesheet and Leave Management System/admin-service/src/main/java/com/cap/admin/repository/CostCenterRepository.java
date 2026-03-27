package com.cap.admin.repository;

import com.cap.admin.entity.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CostCenterRepository
        extends JpaRepository<CostCenter, Long> {

    Optional<CostCenter> findByCodeAndIsActive(
            String code,
            Boolean isActive);

    List<CostCenter> findByIsActive(Boolean isActive);

    boolean existsByCode(String code);
}