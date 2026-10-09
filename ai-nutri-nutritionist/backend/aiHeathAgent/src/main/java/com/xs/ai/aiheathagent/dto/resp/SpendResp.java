package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;


@Data @AllArgsConstructor @NoArgsConstructor
public class SpendResp {
    private String orderNo;
    private BigDecimal amountKc;
    private BigDecimal balanceAfter;
    private String status; // success
}