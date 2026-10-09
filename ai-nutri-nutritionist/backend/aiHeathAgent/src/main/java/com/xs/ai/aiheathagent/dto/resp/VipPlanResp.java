package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class VipPlanResp {
    private String id;
    private String title;
    private String desc;
    private BigDecimal price;
    private String per;
    private Integer days;
    private Boolean supportAutoRenew;
    private String tag;
    private String accent;
    private List<String> highlights;
    private List<String> details;
}

