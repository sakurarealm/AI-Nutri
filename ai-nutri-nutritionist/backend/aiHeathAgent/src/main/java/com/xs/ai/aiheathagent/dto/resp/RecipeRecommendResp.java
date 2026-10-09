package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeRecommendResp {
    private Long memberId;
    private String date;
    private String tips;
    private List<String> focusOrganKeys;
    private List<RecipeCardResp> recipes;
}

