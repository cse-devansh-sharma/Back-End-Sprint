package com.cap.leave.repository;

import com.cap.leave.entity.LeaveRequest;
import com.cap.leave.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByUserIdAndStatus(Long userId, LeaveStatus status, Pageable pageable);

    Page<LeaveRequest> findByUserId( Long userId, Pageable pageable);

    // find all by userId and status in a list
    // e.g. findByUserIdAndStatusIn(1L, [SUBMITTED, APPROVED])
    List<LeaveRequest> findByUserIdAndStatusIn( Long userId, List<LeaveStatus> statuses);

    // find team leaves — userId in a list + status
    List<LeaveRequest> findByUserIdInAndStatus( List<Long> userIds, LeaveStatus status);

    // ── overlap check — too complex for JPA method name ──
    // checks if dates overlap with existing active requests
    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.userId = :userId
            AND lr.status IN :statuses
            AND lr.fromDate <= :toDate
            AND lr.toDate >= :fromDate
            """)
    List<LeaveRequest> findOverlappingLeave(@Param("userId")   Long userId, @Param("fromDate") LocalDate fromDate, @Param("toDate")  LocalDate toDate, @Param("statuses") List<LeaveStatus> statuses);
}