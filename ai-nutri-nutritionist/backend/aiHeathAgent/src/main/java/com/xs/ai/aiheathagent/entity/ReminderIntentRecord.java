package com.xs.ai.aiheathagent.entity;

import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_intent_records", indexes = {
        @Index(name = "idx_reminder_intent_user_key", columnList = "userId,decisionKey", unique = true),
        @Index(name = "idx_reminder_intent_status_until", columnList = "status,validUntil")
})
@Data
public class ReminderIntentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column(nullable = false)
    private String timeSlot;

    @Column(nullable = false)
    private String itemKey;

    @Column(nullable = false)
    private String decisionKey;

    @Column(nullable = false)
    private String windowId;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExplanationCode explanationCode = ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN;

    @Column(columnDefinition = "TEXT")
    private String explanationArgs;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime executedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }
    public String getDecisionKey() { return decisionKey; }
    public void setDecisionKey(String decisionKey) { this.decisionKey = decisionKey; }
    public String getWindowId() { return windowId; }
    public void setWindowId(String windowId) { this.windowId = windowId; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ExplanationCode getExplanationCode() { return explanationCode; }
    public void setExplanationCode(ExplanationCode explanationCode) { this.explanationCode = explanationCode; }
    public String getExplanationArgs() { return explanationArgs; }
    public void setExplanationArgs(String explanationArgs) { this.explanationArgs = explanationArgs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
