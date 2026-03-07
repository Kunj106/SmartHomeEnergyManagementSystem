package com.kunj.SmartHomeEnergyManagementSystem.Repository;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog,Long>
{
    // Latest N logs regardless of user
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    // Logs for a specific user
    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    // Filter by level (INFO / WARN / ERROR)
    Page<AuditLog> findByLevelOrderByTimestampDesc(String level, Pageable pageable);

    // Filter by action keyword
    Page<AuditLog> findByActionContainingIgnoreCaseOrderByTimestampDesc(String action, Pageable pageable);

    // Date range query
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<AuditLog> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Count by level for summary
    long countByLevel(String level);

    // Count today's logs
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp >= :startOfDay")
    long countTodayLogs(@Param("startOfDay") LocalDateTime startOfDay);
}
