package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeDetailResp {
    private Long id;
    private String title;
    private int calories;
    private int timeMinutes;
    private String reason;
    private String image;
    private double carb;
    private double protein;
    private double fat;
    private double fiber;
    private List<Ingredient> ingredients;
    private List<Step> steps;
    private String nutritionValue;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ingredient {
        private String name;
        private String amount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Step {
        private String title;
        private String description;
        private int time;
    }
}

