package com.kunj.SmartHomeEnergyManagementSystem.Service;

import com.kunj.SmartHomeEnergyManagementSystem.Entity.AuditLog;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.AuditLogRepository;
import com.kunj.SmartHomeEnergyManagementSystem.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuditLogService
{
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;


    /** Log an INFO-level event. */
    public AuditLog logInfo(Long userId, String action, String description) {
        return saveLog(userId, action, description, "INFO", null, null, null);
    }

    /** Log a WARN-level event. */
    public AuditLog logWarn(Long userId, String action, String description) {
        return saveLog(userId, action, description, "WARN", null, null, null);
    }

    /** Log an ERROR-level event. */
    public AuditLog logError(Long userId, String action, String description) {
        return saveLog(userId, action, description, "ERROR", null, null, null);
    }

    /** Overloads with entity context (used by AdminController). */
    public AuditLog logInfo(Long userId, String action, String description,
                            String entityType, Long entityId, String ip) {
        return saveLog(userId, action, description, "INFO", entityType, entityId, ip);
    }

    public AuditLog logWarn(Long userId, String action, String description,
                            String entityType, Long entityId, String ip) {
        return saveLog(userId, action, description, "WARN", entityType, entityId, ip);
    }

    /** Full-detail log. */
    public AuditLog logAction(Long userId, String action, String description,
                              String level, String entityType, Long entityId, String ipAddress) {
        return saveLog(userId, action, description, level, entityType, entityId, ipAddress);
    }

    @Transactional
    private AuditLog saveLog(Long userId, String action, String description,
                             String level, String entityType, Long entityId, String ipAddress) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .action(action)
                .description(description)
                .level(level != null ? level : "INFO")
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress);

        if (userId != null) {
            userRepository.findById(userId).ifPresent(builder::user);
        }

        AuditLog saved = auditLogRepository.save(builder.build());
        log.info("[AUDIT] [{}] {} - {}", level, action, description);
        return saved;
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /** Paginated: all logs newest-first. Default page size = 50. */
    public Page<AuditLogDTO> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::toDTO);
    }

    /** Filter by level. */
    public Page<AuditLogDTO> getLogsByLevel(String level, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByLevelOrderByTimestampDesc(level, pageable)
                .map(this::toDTO);
    }

    /** Filter by action keyword. */
    public Page<AuditLogDTO> getLogsByAction(String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByActionContainingIgnoreCaseOrderByTimestampDesc(action, pageable)
                .map(this::toDTO);
    }

    /** Date-range query (for reports). */
    public List<AuditLogDTO> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByDateRange(start, end)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Summary counts used in dashboard / report header. */
    public LogSummaryDTO getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return new LogSummaryDTO(
                auditLogRepository.count(),
                auditLogRepository.countByLevel("INFO"),
                auditLogRepository.countByLevel("WARN"),
                auditLogRepository.countByLevel("ERROR"),
                auditLogRepository.countTodayLogs(startOfToday)
        );
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    private AuditLogDTO toDTO(AuditLog log) {
        String username = log.getUser() != null ? log.getUser().getUsername() : "system";
        return new AuditLogDTO(
                log.getId(),
                username,
                log.getAction(),
                log.getDescription(),
                log.getEntityType(),
                log.getEntityId(),
                log.getIpAddress(),
                log.getLevel(),
                log.getTimestamp()
        );
    }

    public record AuditLogDTO(
            Long id,
            String username,
            String action,
            String description,
            String entityType,
            Long entityId,
            String ipAddress,
            String level,
            LocalDateTime timestamp
    ) {}

    public record LogSummaryDTO(
            long total,
            long infoCount,
            long warnCount,
            long errorCount,
            long todayCount
    ) {}
}
