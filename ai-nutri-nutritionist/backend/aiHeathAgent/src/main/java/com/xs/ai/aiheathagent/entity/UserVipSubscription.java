package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_vip_subscriptions")
@Data
public class UserVipSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "plan_id", nullable = false)
    private String planId;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew;

    @Column(name = "family_seats", nullable = false)
    private Integer familySeats;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

