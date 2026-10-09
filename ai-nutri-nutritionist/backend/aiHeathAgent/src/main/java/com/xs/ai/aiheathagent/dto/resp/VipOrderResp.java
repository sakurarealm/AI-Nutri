package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class VipOrderResp {
    private Long id;
    private String orderNo;
    private String planId;
    private Integer months;
    private Integer familySeats;
    private Boolean autoRenew;
    private BigDecimal amountCny;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}

