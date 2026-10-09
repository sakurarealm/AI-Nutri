package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
@Entity
@Table(name = "auth_refresh_tokens")
@Data
@Access(AccessType.FIELD) // 强制字段访问方式
public class AuthRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;



}

