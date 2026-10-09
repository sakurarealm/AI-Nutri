package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;


@Data @AllArgsConstructor @NoArgsConstructor
public class CreateOrderResp {
    private String orderNo;
    private String payChannel;
    private BigDecimal amount;
    private Integer kcoins; // 基础 K币
    private Integer gift;
    private String prepayId; // 用于真实支付跳转（mock 下给一个字符串）
    private String status; // pending
}