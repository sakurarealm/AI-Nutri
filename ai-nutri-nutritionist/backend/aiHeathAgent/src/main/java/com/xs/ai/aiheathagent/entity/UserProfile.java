package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity @Table(name = "user_profiles") @Data
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private Long userId;


    @Column(name = "height_cm") private BigDecimal heightCm; // 身高（厘米）
    @Column(name = "target_weight_kg") private BigDecimal targetWeightKg; // 目标体重（kg）
    @Column(name = "last_weight_kg") private BigDecimal lastWeightKg; // 最近一次体重（kg）


    private String sex; // optional: male/female/other
    private LocalDate birthday; // optional


    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getTargetWeightKg() { return targetWeightKg; }
    public void setTargetWeightKg(BigDecimal targetWeightKg) { this.targetWeightKg = targetWeightKg; }
    public BigDecimal getLastWeightKg() { return lastWeightKg; }
    public void setLastWeightKg(BigDecimal lastWeightKg) { this.lastWeightKg = lastWeightKg; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
