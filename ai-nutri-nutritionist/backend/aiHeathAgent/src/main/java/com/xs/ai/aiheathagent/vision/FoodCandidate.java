package com.xs.ai.aiheathagent.vision;

import lombok.Data;

@Data
public class FoodCandidate {
    private String name;
    private Double probability;
    private Double calorie;
    private Double carb;
    private Double protein;
    private Double fat;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }
    public Double getCalorie() { return calorie; }
    public void setCalorie(Double calorie) { this.calorie = calorie; }
    public Double getCarb() { return carb; }
    public void setCarb(Double carb) { this.carb = carb; }
    public Double getProtein() { return protein; }
    public void setProtein(Double protein) { this.protein = protein; }
    public Double getFat() { return fat; }
    public void setFat(Double fat) { this.fat = fat; }
}
