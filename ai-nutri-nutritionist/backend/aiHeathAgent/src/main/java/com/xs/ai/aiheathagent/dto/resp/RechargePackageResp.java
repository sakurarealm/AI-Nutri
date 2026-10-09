package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;


@Data @AllArgsConstructor @NoArgsConstructor
public class RechargePackageResp {
    private Long id;
    private BigDecimal price; // ¥
    private Integer kcoins;
    private Integer gift;
    private boolean isRecommend;
}