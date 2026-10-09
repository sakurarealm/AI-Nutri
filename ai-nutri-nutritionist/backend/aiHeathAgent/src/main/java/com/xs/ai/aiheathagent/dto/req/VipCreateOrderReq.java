package com.xs.ai.aiheathagent.dto.req;

import lombok.Data;

@Data
public class VipCreateOrderReq {
    private String planId;
    private Integer months;
    private Integer familySeats;
    private Boolean autoRenew;
}

