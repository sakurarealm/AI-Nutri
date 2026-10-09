package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_plan_records", indexes = {
        @Index(name = "idx_daily_plan_user_date", columnList = "userId,planDate", unique = true)
})
@Data
public class DailyPlanRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column(nullable = false)
    private String planId;

    @Column(nullable = false)
    private boolean variationFlag;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String planJson;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public boolean isVariationFlag() { return variationFlag; }
    public void setVariationFlag(boolean variationFlag) { this.variationFlag = variationFlag; }
    public String getPlanJson() { return planJson; }
    public void setPlanJson(String planJson) { this.planJson = planJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
