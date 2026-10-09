package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_capability_change_records", indexes = {
        @Index(name = "idx_capability_change_user_time", columnList = "userId,createdAt"),
        @Index(name = "idx_capability_change_user_key", columnList = "userId,capabilityKey")
})
@Data
public class UserCapabilityChangeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityKey capabilityKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityState fromState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapabilityState toState;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

