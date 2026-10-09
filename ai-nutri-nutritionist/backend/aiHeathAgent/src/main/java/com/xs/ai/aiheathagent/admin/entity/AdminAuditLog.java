package com.xs.ai.aiheathagent.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
@Data
public class AdminAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long operatorAdminId;
    private String operatorAdminUid;
    private Integer operatorRoleLevel;
    private String operatorBizLine;

    private String action;
    private String targetType;
    private String targetId;

    private String ip;
    private String result;

    @Column(columnDefinition = "text")
    private String detail;

    private LocalDateTime createdAt;
}

