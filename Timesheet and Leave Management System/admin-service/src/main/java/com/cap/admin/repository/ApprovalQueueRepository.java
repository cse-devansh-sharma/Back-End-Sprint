package com.cap.admin.repository;

import com.cap.admin.entity.ApprovalQueue;
import com.cap.admin.enums.ApprovalStatus;
import com.cap.admin.enums.ReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalQueueRepository
        extends JpaRepository<ApprovalQueue, Long> {

    // get all pending approvals for a manager
    Page<ApprovalQueue> findByAssignedToAndStatus(
            Long assignedTo,
            ApprovalStatus status,
            Pageable pageable);

    // find specific item by reference
    Optional<ApprovalQueue> findByReferenceIdAndReferenceType(
            Long referenceId,
            ReferenceType referenceType);

    // idempotency check
    boolean existsByReferenceIdAndReferenceTypeAndStatus(
            Long referenceId,
            ReferenceType referenceType,
            ApprovalStatus status);

    // get all items for a specific employee
    List<ApprovalQueue> findByRequestedBy(Long requestedBy);

    // count pending for dashboard
    long countByAssignedToAndStatus(
            Long assignedTo,
            ApprovalStatus status);
}