package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeCardResp {
    private Long id;
    private String title;
    private int calories;
    private int timeMinutes;
    private String reason;
    private String image;
}

