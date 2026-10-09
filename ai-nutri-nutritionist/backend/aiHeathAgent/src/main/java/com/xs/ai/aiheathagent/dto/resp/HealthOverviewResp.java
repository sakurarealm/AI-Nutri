package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;


@Data @AllArgsConstructor @NoArgsConstructor
public class HealthOverviewResp {
    private String lastUpdateTime; // 文案，如：今天 14:30
    private BigDecimal bmi; // BMI
    private String bmiTrend; // up/down/flat
    private BigDecimal weight; // kg
    private String weightTrend; // up/down/flat
    private Integer continuousDays; // 连续打卡天数
    private String healthLevel; // Lv.1 ~ Lv.5
}
