package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodCatalogItemResp {
    private Long id;
    private String name;
    private String unit;
    private String nutrient;
    private int calorie;
    private double carb;
    private double protein;
    private double fat;
    private double fiber;
    private String img;
    private String note;

    private String baseUnit;
    private double baseAmount;
    private double baseFiber;
    private double portionMultiplier;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getNutrient() { return nutrient; }
    public void setNutrient(String nutrient) { this.nutrient = nutrient; }
    public int getCalorie() { return calorie; }
    public void setCalorie(int calorie) { this.calorie = calorie; }
    public double getCarb() { return carb; }
    public void setCarb(double carb) { this.carb = carb; }
    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }
    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }
    public double getFiber() { return fiber; }
    public void setFiber(double fiber) { this.fiber = fiber; }
    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getBaseUnit() { return baseUnit; }
    public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }
    public double getBaseAmount() { return baseAmount; }
    public void setBaseAmount(double baseAmount) { this.baseAmount = baseAmount; }
    public double getBaseFiber() { return baseFiber; }
    public void setBaseFiber(double baseFiber) { this.baseFiber = baseFiber; }
    public double getPortionMultiplier() { return portionMultiplier; }
    public void setPortionMultiplier(double portionMultiplier) { this.portionMultiplier = portionMultiplier; }
}
