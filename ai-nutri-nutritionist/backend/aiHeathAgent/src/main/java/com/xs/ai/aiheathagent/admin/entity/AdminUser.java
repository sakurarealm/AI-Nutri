package com.xs.ai.aiheathagent.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_users")
@Data
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uid;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String nickname;
    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String avatarUrl;
    private String passwordHash;
    private String role;
    private Integer roleLevel;
    private String bizLine;
    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private Integer failedLoginCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

