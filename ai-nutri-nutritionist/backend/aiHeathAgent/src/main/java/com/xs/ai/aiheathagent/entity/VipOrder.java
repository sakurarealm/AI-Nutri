package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vip_orders")
@Data
public class VipOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", unique = true, nullable = false)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_id", nullable = false)
    private String planId;

    @Column(name = "months", nullable = false)
    private Integer months;

    @Column(name = "family_seats", nullable = false)
    private Integer familySeats;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew;

    @Column(name = "amount_cny", nullable = false)
    private BigDecimal amountCny;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "notify_payload", columnDefinition = "json")
    private String notifyPayload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

