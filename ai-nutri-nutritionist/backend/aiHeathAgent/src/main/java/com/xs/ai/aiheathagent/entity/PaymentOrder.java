package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity @Table(name="payment_orders") @Data
public class PaymentOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="order_no", unique = true, nullable=false) private String orderNo;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="package_id") private Long packageId;
    @Column(name="pay_channel", nullable=false) private String payChannel; // wechat/alipay/apple/test
    @Column(name="amount_cny", nullable=false) private BigDecimal amountCny;
    private Integer kcoins; // 不含 gift 的基础 K币
    private String status; // pending/paid/failed/closed/refunded
    @Column(name="prepay_id") private String prepayId;
    @Column(name="paid_at") private LocalDateTime paidAt;
    @Column(name="notify_payload", columnDefinition = "json") private String notifyPayload;
    @Column(name="created_at") private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
}