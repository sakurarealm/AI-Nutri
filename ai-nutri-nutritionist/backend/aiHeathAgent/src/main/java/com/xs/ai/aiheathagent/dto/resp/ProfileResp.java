package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Data @AllArgsConstructor @NoArgsConstructor
public class ProfileResp {
    private BigDecimal heightCm; private BigDecimal targetWeightKg; private BigDecimal lastWeightKg;
    private String sex; private LocalDate birthday;
}
