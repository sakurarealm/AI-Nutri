package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity @Table(name = "refund_orders", indexes = {
        @Index(name = "idx_ro_user", columnList = "user_id"),
        @Index(name = "idx_ro_ref_spend", columnList = "spend_order_id"),
        @Index(name = "idx_ro_refund_no", columnList = "refund_no", unique = true),
        @Index(name = "idx_ro_idem", columnList = "idempotent_key", unique = true)
})
@Data
public class RefundOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "refund_no", nullable = false, unique = true)
    private String refundNo; // RFyyyyMMddHHmmss + 随机


    @Column(name = "user_id", nullable = false)
    private Long userId;


    @Column(name = "spend_order_id", nullable = false)
    private Long spendOrderId; // 对应的消费单


    @Column(name = "amount_kc", nullable = false)
    private BigDecimal amountKc; // 正数：本次退款 K币数


    @Column(name = "idempotent_key", nullable = false, unique = true)
    private String idempotentKey; // 幂等键


    // success / failed
    private String status;


    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}