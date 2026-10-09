package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity @Table(name="wallet_accounts") @Data
public class WalletAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="user_id", nullable=false, unique = true)
    private Long userId;
    @Column(name="balance_kc", nullable=false)
    private BigDecimal balanceKc;
    @Column(name="total_recharge", nullable=false)
    private BigDecimal totalRecharge;
    @Column(name="total_spent", nullable=false)
    private BigDecimal totalSpent;
    @Column(name="updated_at")
    private LocalDateTime updatedAt;
}