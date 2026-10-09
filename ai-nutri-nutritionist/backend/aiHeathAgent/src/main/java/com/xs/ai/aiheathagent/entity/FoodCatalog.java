package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "food_catalogs",
        indexes = {
                @Index(name = "idx_food_catalogs_active", columnList = "isActive,sortOrder,id"),
                @Index(name = "idx_food_catalogs_name", columnList = "name")
        }
)
@Data
public class FoodCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 512)
    private String keywords = "";

    @Column(nullable = false, length = 64)
    private String mealTags = "";

    @Column(nullable = false, length = 16)
    private String baseUnit = "g";

    @Column(nullable = false)
    private Double baseAmount = 100.0;

    @Column(nullable = false)
    private Integer baseCalorie = 0;

    @Column(nullable = false)
    private Double baseCarb = 0.0;

    @Column(nullable = false)
    private Double baseProtein = 0.0;

    @Column(nullable = false)
    private Double baseFat = 0.0;

    @Column(nullable = false)
    private Double baseFiber = 0.0;

    @Column(nullable = false, length = 512)
    private String imgUrl = "";

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Integer isActive = 1;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getMealTags() { return mealTags; }
    public void setMealTags(String mealTags) { this.mealTags = mealTags; }
    public String getBaseUnit() { return baseUnit; }
    public void setBaseUnit(String baseUnit) { this.baseUnit = baseUnit; }
    public Double getBaseAmount() { return baseAmount; }
    public void setBaseAmount(Double baseAmount) { this.baseAmount = baseAmount; }
    public Integer getBaseCalorie() { return baseCalorie; }
    public void setBaseCalorie(Integer baseCalorie) { this.baseCalorie = baseCalorie; }
    public Double getBaseCarb() { return baseCarb; }
    public void setBaseCarb(Double baseCarb) { this.baseCarb = baseCarb; }
    public Double getBaseProtein() { return baseProtein; }
    public void setBaseProtein(Double baseProtein) { this.baseProtein = baseProtein; }
    public Double getBaseFat() { return baseFat; }
    public void setBaseFat(Double baseFat) { this.baseFat = baseFat; }
    public Double getBaseFiber() { return baseFiber; }
    public void setBaseFiber(Double baseFiber) { this.baseFiber = baseFiber; }
    public String getImgUrl() { return imgUrl; }
    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
