package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.util.List;


@Data @AllArgsConstructor @NoArgsConstructor
public class WeightTrendResp {
    private List<String> categories; // ['9/1','9/2',...]
    private List<java.math.BigDecimal> series; // [66.5, 66.2, ...]
}
