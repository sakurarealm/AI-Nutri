package com.xs.ai.aiheathagent.dto.req;

import lombok.Data;

@Data
public class FoodPortionCalcReq {
    private Long foodId;
    private Double multiplier;
    private String unitLabel;

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public Double getMultiplier() { return multiplier; }
    public void setMultiplier(Double multiplier) { this.multiplier = multiplier; }
    public String getUnitLabel() { return unitLabel; }
    public void setUnitLabel(String unitLabel) { this.unitLabel = unitLabel; }
}

