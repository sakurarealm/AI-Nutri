package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "decision_consume_records", indexes = {
        @Index(name = "idx_decision_consume_user_key", columnList = "userId,decisionKey", unique = true)
})
@Data
public class DecisionConsumeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String decisionKey;

    @Column(nullable = false)
    private LocalDateTime consumedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDecisionKey() { return decisionKey; }
    public void setDecisionKey(String decisionKey) { this.decisionKey = decisionKey; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
}
