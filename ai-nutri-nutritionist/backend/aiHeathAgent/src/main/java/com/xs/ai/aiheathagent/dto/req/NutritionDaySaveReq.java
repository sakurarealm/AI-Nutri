package com.xs.ai.aiheathagent.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class NutritionDaySaveReq {
    @NotNull(message = "memberId 不能为空")
    private Long memberId;

    private String date;

    private Integer calorieTarget;
    private Double carbTarget;
    private Double proteinTarget;
    private Double fatTarget;

    private List<NutritionFoodItemReq> breakfastFoods;
    private List<NutritionFoodItemReq> lunchFoods;
    private List<NutritionFoodItemReq> dinnerFoods;
    private List<NutritionSnackCardReq> snackCards;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Integer getCalorieTarget() { return calorieTarget; }
    public void setCalorieTarget(Integer calorieTarget) { this.calorieTarget = calorieTarget; }
    public Double getCarbTarget() { return carbTarget; }
    public void setCarbTarget(Double carbTarget) { this.carbTarget = carbTarget; }
    public Double getProteinTarget() { return proteinTarget; }
    public void setProteinTarget(Double proteinTarget) { this.proteinTarget = proteinTarget; }
    public Double getFatTarget() { return fatTarget; }
    public void setFatTarget(Double fatTarget) { this.fatTarget = fatTarget; }
    public List<NutritionFoodItemReq> getBreakfastFoods() { return breakfastFoods; }
    public void setBreakfastFoods(List<NutritionFoodItemReq> breakfastFoods) { this.breakfastFoods = breakfastFoods; }
    public List<NutritionFoodItemReq> getLunchFoods() { return lunchFoods; }
    public void setLunchFoods(List<NutritionFoodItemReq> lunchFoods) { this.lunchFoods = lunchFoods; }
    public List<NutritionFoodItemReq> getDinnerFoods() { return dinnerFoods; }
    public void setDinnerFoods(List<NutritionFoodItemReq> dinnerFoods) { this.dinnerFoods = dinnerFoods; }
    public List<NutritionSnackCardReq> getSnackCards() { return snackCards; }
    public void setSnackCards(List<NutritionSnackCardReq> snackCards) { this.snackCards = snackCards; }
}

