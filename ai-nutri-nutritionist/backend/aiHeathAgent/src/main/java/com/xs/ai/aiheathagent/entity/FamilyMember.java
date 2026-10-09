package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "family_members",
        indexes = {
                @Index(name = "idx_family_members_user_id", columnList = "userId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_family_members_user_key", columnNames = {"userId", "memberKey"})
        }
)
@Data
public class FamilyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String memberKey;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(nullable = false, length = 32)
    private String relation;

    @Column(nullable = false)
    private Integer calorieTarget = 2000;

    @Column(nullable = false)
    private Double carbTarget = 264.0;

    @Column(nullable = false)
    private Double proteinTarget = 100.0;

    @Column(nullable = false)
    private Double fatTarget = 67.0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMemberKey() { return memberKey; }
    public void setMemberKey(String memberKey) { this.memberKey = memberKey; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }
    public Integer getCalorieTarget() { return calorieTarget; }
    public void setCalorieTarget(Integer calorieTarget) { this.calorieTarget = calorieTarget; }
    public Double getCarbTarget() { return carbTarget; }
    public void setCarbTarget(Double carbTarget) { this.carbTarget = carbTarget; }
    public Double getProteinTarget() { return proteinTarget; }
    public void setProteinTarget(Double proteinTarget) { this.proteinTarget = proteinTarget; }
    public Double getFatTarget() { return fatTarget; }
    public void setFatTarget(Double fatTarget) { this.fatTarget = fatTarget; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
