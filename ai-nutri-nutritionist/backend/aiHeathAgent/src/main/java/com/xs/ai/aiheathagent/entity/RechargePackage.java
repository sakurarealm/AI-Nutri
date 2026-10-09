package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;


@Entity @Table(name="recharge_packages") @Data
public class RechargePackage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name="price_cny") private java.math.BigDecimal priceCny;
    private Integer kcoins;
    private Integer gift;
    @Column(name="is_recommend") private Integer isRecommend; // 0/1
    private Integer status; // 1 启用
    @Column(name="sort_no") private Integer sortNo;
}