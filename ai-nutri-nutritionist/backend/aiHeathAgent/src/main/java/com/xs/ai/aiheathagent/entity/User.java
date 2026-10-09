package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="users") @Data
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true) private String uid;
    @Column(unique=true) private String phone;
    @Column(unique=true) private String email;

    // 【新增】微信OpenID字段
    @Column(unique=true) private String openid;

    private String nickname;
    private String passwordHash;
    private String avatarUrl;
    private Integer gender; // 0/1/2
    private LocalDate birthDate;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String role; // USER/ADMIN
    private Integer status; // 1/0
    private String bizLine;
    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getBizLine() { return bizLine; }
    public void setBizLine(String bizLine) { this.bizLine = bizLine; }
    public Long getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(Long createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
