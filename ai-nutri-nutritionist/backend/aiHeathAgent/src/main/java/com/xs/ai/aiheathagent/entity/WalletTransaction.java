package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "wallet_transactions")
@Data
public class WalletTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String type;                 // recharge / spend / refund / adjust
    @Column(name = "change_kc", nullable = false)
    private BigDecimal changeKc;         // 变更的K币（可正可负）
    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;     // 变更后的余额
    @Column(name = "ref_order_id")
    private Long refOrderId;             // 关联订单ID（可空）
    private String memo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
