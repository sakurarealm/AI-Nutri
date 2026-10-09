package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NutritionSnackCardResp {
    private String id;
    private int level;
    private List<NutritionFoodItemResp> foods;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public List<NutritionFoodItemResp> getFoods() { return foods; }
    public void setFoods(List<NutritionFoodItemResp> foods) { this.foods = foods; }
}

