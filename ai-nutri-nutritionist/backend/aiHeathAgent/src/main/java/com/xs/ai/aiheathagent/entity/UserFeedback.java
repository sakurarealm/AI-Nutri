package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_feedbacks", indexes = {
        @Index(name = "idx_uf_user", columnList = "user_id"),
        @Index(name = "idx_uf_status_created", columnList = "status,created_at"),
        @Index(name = "idx_uf_created", columnList = "created_at")
})
@Data
public class UserFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String subject;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "order_no", length = 64)
    private String orderNo;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "admin_remark", length = 2000)
    private String adminRemark;

    @Column(name = "handled_by_admin_id")
    private Long handledByAdminId;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

