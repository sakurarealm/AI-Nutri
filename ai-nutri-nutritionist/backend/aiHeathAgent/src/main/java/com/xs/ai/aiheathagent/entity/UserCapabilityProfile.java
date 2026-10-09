package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_capability_profiles", indexes = {
        @Index(name = "idx_user_capability_user", columnList = "userId", unique = true)
})
@Data
public class UserCapabilityProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityState medicationReminderEnabled = CapabilityState.OFF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityState supplementReminderEnabled = CapabilityState.OFF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityState exerciseSuggestionEnabled = CapabilityState.OFF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityState proactiveSummaryEnabled = CapabilityState.ENABLED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityState proactiveReminderEnabled = CapabilityState.OFF;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public CapabilityState getMedicationReminderEnabled() { return medicationReminderEnabled; }
    public void setMedicationReminderEnabled(CapabilityState medicationReminderEnabled) { this.medicationReminderEnabled = medicationReminderEnabled; }
    public CapabilityState getSupplementReminderEnabled() { return supplementReminderEnabled; }
    public void setSupplementReminderEnabled(CapabilityState supplementReminderEnabled) { this.supplementReminderEnabled = supplementReminderEnabled; }
    public CapabilityState getExerciseSuggestionEnabled() { return exerciseSuggestionEnabled; }
    public void setExerciseSuggestionEnabled(CapabilityState exerciseSuggestionEnabled) { this.exerciseSuggestionEnabled = exerciseSuggestionEnabled; }
    public CapabilityState getProactiveSummaryEnabled() { return proactiveSummaryEnabled; }
    public void setProactiveSummaryEnabled(CapabilityState proactiveSummaryEnabled) { this.proactiveSummaryEnabled = proactiveSummaryEnabled; }
    public CapabilityState getProactiveReminderEnabled() { return proactiveReminderEnabled; }
    public void setProactiveReminderEnabled(CapabilityState proactiveReminderEnabled) { this.proactiveReminderEnabled = proactiveReminderEnabled; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
