package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NutritionDayResp {
    private Long memberId;
    private String date;
    private int calorieConsumed;
    private int calorieTarget;
    private Map<String, Double> macroTargets;
    private Map<String, Double> macroTotals;
    private Map<String, Integer> macroPercent;
    private List<NutritionFoodItemResp> breakfastFoods;
    private List<NutritionFoodItemResp> lunchFoods;
    private List<NutritionFoodItemResp> dinnerFoods;
    private List<NutritionSnackCardResp> snackCards;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int getCalorieConsumed() { return calorieConsumed; }
    public void setCalorieConsumed(int calorieConsumed) { this.calorieConsumed = calorieConsumed; }
    public int getCalorieTarget() { return calorieTarget; }
    public void setCalorieTarget(int calorieTarget) { this.calorieTarget = calorieTarget; }
    public Map<String, Double> getMacroTargets() { return macroTargets; }
    public void setMacroTargets(Map<String, Double> macroTargets) { this.macroTargets = macroTargets; }
    public Map<String, Double> getMacroTotals() { return macroTotals; }
    public void setMacroTotals(Map<String, Double> macroTotals) { this.macroTotals = macroTotals; }
    public Map<String, Integer> getMacroPercent() { return macroPercent; }
    public void setMacroPercent(Map<String, Integer> macroPercent) { this.macroPercent = macroPercent; }
    public List<NutritionFoodItemResp> getBreakfastFoods() { return breakfastFoods; }
    public void setBreakfastFoods(List<NutritionFoodItemResp> breakfastFoods) { this.breakfastFoods = breakfastFoods; }
    public List<NutritionFoodItemResp> getLunchFoods() { return lunchFoods; }
    public void setLunchFoods(List<NutritionFoodItemResp> lunchFoods) { this.lunchFoods = lunchFoods; }
    public List<NutritionFoodItemResp> getDinnerFoods() { return dinnerFoods; }
    public void setDinnerFoods(List<NutritionFoodItemResp> dinnerFoods) { this.dinnerFoods = dinnerFoods; }
    public List<NutritionSnackCardResp> getSnackCards() { return snackCards; }
    public void setSnackCards(List<NutritionSnackCardResp> snackCards) { this.snackCards = snackCards; }
}

