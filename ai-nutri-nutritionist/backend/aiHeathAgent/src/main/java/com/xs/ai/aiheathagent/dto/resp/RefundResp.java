package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;


@Data @AllArgsConstructor @NoArgsConstructor
public class RefundResp {
    private String refundNo;
    private String orderNo; // 被退款的消费单
    private BigDecimal refundKc; // 本次退款
    private BigDecimal balanceAfter;
    private String status; // success
}