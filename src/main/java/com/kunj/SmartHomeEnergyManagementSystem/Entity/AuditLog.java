package com.kunj.SmartHomeEnergyManagementSystem.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who performed the action (null = system)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // The action type e.g. USER_LOGIN, USER_DELETED, REPORT_GENERATED, BACKUP_STARTED
    @Column(nullable = false, length = 100)
    private String action;

    // Human-readable description
    @Column(length = 500)
    private String description;

    // Entity that was affected (optional)
    @Column(name = "entity_type", length = 50)
    private String entityType; // "User", "Device", "EnergyLog", etc.

    @Column(name = "entity_id")
    private Long entityId;

    // HTTP or system level info
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // INFO, WARN, ERROR
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String level = "INFO";

    // Extra JSON data (optional)
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    //  Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
