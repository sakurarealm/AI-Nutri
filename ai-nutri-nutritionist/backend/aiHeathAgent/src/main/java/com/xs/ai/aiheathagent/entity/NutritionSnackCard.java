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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "nutrition_snack_cards",
        indexes = {
                @Index(name = "idx_nutrition_snack_cards_day", columnList = "userId,memberId,recordDate")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_nutrition_snack_cards_day_card", columnNames = {"userId", "memberId", "recordDate", "cardId"})
        }
)
@Data
public class NutritionSnackCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false, length = 128)
    private String cardId;

    @Column(nullable = false)
    private Integer level = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

