package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity @Table(name = "spend_orders", indexes = {
        @Index(name = "idx_so_user", columnList = "user_id"),
        @Index(name = "idx_so_order_no", columnList = "order_no", unique = true),
        @Index(name = "idx_so_idem", columnList = "idempotent_key", unique = true)
})
@Data
public class SpendOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo; // SOyyyyMMddHHmmss + 随机


    @Column(name = "user_id", nullable = false)
    private Long userId;


    @Column(name = "amount_kc", nullable = false)
    private BigDecimal amountKc; // 正数，代表本次应扣的 K币数量


    @Column(name = "biz_type")
    private String bizType; // 业务类型（如: ai_chat, plan, vip）


    @Column(name = "biz_ref")
    private String bizRef; // 业务外部关联 ID


    @Column(name = "idempotent_key", nullable = false, unique = true)
    private String idempotentKey; // 幂等键


    // success / partial_refunded / refunded
    private String status;


    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}