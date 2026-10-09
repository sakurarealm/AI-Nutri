package com.xs.ai.aiheathagent.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class NutritionSnackCardReq {
    private String id;
    private Integer level;
    private List<NutritionFoodItemReq> foods;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public List<NutritionFoodItemReq> getFoods() { return foods; }
    public void setFoods(List<NutritionFoodItemReq> foods) { this.foods = foods; }
}

