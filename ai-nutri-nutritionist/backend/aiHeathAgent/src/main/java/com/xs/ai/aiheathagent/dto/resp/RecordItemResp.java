package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;


@Data @AllArgsConstructor @NoArgsConstructor
public class RecordItemResp {
    private Long id; private String type; private java.math.BigDecimal value1; private java.math.BigDecimal value2;
    private String unit1; private String unit2; private String note; private String ext; private java.time.LocalDateTime recordedAt;
}
