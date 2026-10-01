package com.example.resilience.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String actor;
    @Column(nullable = false) private String action;
    @Column(name = "resource_type", nullable = false) private String resourceType;
    @Column(name = "resource_id") private String resourceId;
    private String ip;
    @Column(columnDefinition = "json") private String detail;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;

    protected AuditLog() {}
    public AuditLog(String actor, String action, String resourceType, String resourceId, String ip, String detail) {
        this.actor = actor; this.action = action; this.resourceType = resourceType;
        this.resourceId = resourceId; this.ip = ip; this.detail = detail;
    }
}
