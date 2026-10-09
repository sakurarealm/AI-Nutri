package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.time.*;


@Entity @Table(name="verification_codes") @Data
public class VerificationCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String scene; // register/login/reset_password
    private String target; // phone or email
    private String code;
    private LocalDateTime expireAt;
    private LocalDateTime usedAt;
    private Integer attemptCount;
    private Integer maxAttempts;
    private String ip;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
