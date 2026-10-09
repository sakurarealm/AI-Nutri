package com.xs.ai.aiheathagent.dto.req;

import lombok.Data;

@Data
public class NutritionFoodItemReq {
    private String name;
    private String unit;
    private Integer calorie;
    private String nutrient;
    private Double carb;
    private Double protein;
    private Double fat;
    private Double fiber;
    private String img;
    private String note;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Integer getCalorie() { return calorie; }
    public void setCalorie(Integer calorie) { this.calorie = calorie; }
    public String getNutrient() { return nutrient; }
    public void setNutrient(String nutrient) { this.nutrient = nutrient; }
    public Double getCarb() { return carb; }
    public void setCarb(Double carb) { this.carb = carb; }
    public Double getProtein() { return protein; }
    public void setProtein(Double protein) { this.protein = protein; }
    public Double getFat() { return fat; }
    public void setFat(Double fat) { this.fat = fat; }
    public Double getFiber() { return fiber; }
    public void setFiber(Double fiber) { this.fiber = fiber; }
    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
