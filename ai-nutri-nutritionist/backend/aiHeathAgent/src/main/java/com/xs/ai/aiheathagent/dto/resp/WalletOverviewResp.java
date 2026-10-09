package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;


@Data @AllArgsConstructor @NoArgsConstructor
public class WalletOverviewResp {
    private BigDecimal balanceKc; // 余额 K币
    private BigDecimal totalRecharge; // 累计充值（¥）
    private BigDecimal totalSpent; // 累计消费（K币）
}