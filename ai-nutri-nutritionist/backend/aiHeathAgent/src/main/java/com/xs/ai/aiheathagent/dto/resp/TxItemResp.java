package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data @AllArgsConstructor @NoArgsConstructor
public class TxItemResp {
    private Long id;
    private String type;
    private BigDecimal changeKc;
    private BigDecimal balanceAfter;
    private String memo;
    private LocalDateTime createdAt;
}