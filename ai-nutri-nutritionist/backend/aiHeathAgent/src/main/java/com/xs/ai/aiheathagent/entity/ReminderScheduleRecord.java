package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_schedule_records", indexes = {
        @Index(name = "idx_reminder_schedule_user_key", columnList = "userId,scheduleKey", unique = true),
        @Index(name = "idx_reminder_schedule_user_enabled", columnList = "userId,enabled")
})
@Data
public class ReminderScheduleRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String scheduleKey;

    @Column(nullable = false)
    private String timeSlot;

    @Column(nullable = false)
    private String itemKey;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}

