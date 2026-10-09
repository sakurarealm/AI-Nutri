package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "nutrition_meal_records",
        indexes = {
                @Index(name = "idx_nutrition_meal_records_day", columnList = "userId,memberId,recordDate,mealType"),
                @Index(name = "idx_nutrition_meal_records_snack", columnList = "userId,memberId,recordDate,snackCardId")
        }
)
@Data
public class NutritionMealRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NutritionMealType mealType;

    @Column(nullable = false, length = 128)
    private String foodName;

    @Column(nullable = false, length = 64)
    private String unit = "";

    @Column(nullable = false, length = 255)
    private String nutrientText = "";

    @Column(nullable = false)
    private Integer calorie = 0;

    @Column(nullable = false)
    private Double carb = 0.0;

    @Column(nullable = false)
    private Double protein = 0.0;

    @Column(nullable = false)
    private Double fat = 0.0;

    @Column(nullable = false)
    private Double fiber = 0.0;

    @Column(nullable = false, length = 512)
    private String imageUrl = "";

    @Column(nullable = false, length = 512)
    private String note = "";

    @Column(nullable = false, length = 128)
    private String snackCardId = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NutritionRecordSource source = NutritionRecordSource.MANUAL;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public NutritionMealType getMealType() { return mealType; }
    public void setMealType(NutritionMealType mealType) { this.mealType = mealType; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getNutrientText() { return nutrientText; }
    public void setNutrientText(String nutrientText) { this.nutrientText = nutrientText; }
    public Integer getCalorie() { return calorie; }
    public void setCalorie(Integer calorie) { this.calorie = calorie; }
    public Double getCarb() { return carb; }
    public void setCarb(Double carb) { this.carb = carb; }
    public Double getProtein() { return protein; }
    public void setProtein(Double protein) { this.protein = protein; }
    public Double getFat() { return fat; }
    public void setFat(Double fat) { this.fat = fat; }
    public Double getFiber() { return fiber; }
    public void setFiber(Double fiber) { this.fiber = fiber; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getSnackCardId() { return snackCardId; }
    public void setSnackCardId(String snackCardId) { this.snackCardId = snackCardId; }
    public NutritionRecordSource getSource() { return source; }
    public void setSource(NutritionRecordSource source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
