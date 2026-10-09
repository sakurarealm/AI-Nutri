package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "recipes",
        indexes = {
                @Index(name = "idx_recipes_active", columnList = "isActive,sortOrder,id"),
                @Index(name = "idx_recipes_title", columnList = "title"),
                @Index(name = "idx_recipes_organs", columnList = "organKeys")
        }
)
@Data
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title = "";

    @Column(nullable = false, length = 512)
    private String imageUrl = "";

    @Column(nullable = false)
    private Integer calories = 0;

    @Column(nullable = false)
    private Integer timeMinutes = 0;

    @Column(nullable = false)
    private Double carbG = 0.0;

    @Column(nullable = false)
    private Double proteinG = 0.0;

    @Column(nullable = false)
    private Double fatG = 0.0;

    @Column(nullable = false)
    private Double fiberG = 0.0;

    @Column(nullable = false, length = 512)
    private String defaultReason = "";

    @Column(nullable = false, length = 2048)
    private String valueText = "";

    @Column(nullable = false, length = 256)
    private String organKeys = "";

    @Column(nullable = false, length = 256)
    private String tags = "";

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_ingredients", joinColumns = @JoinColumn(name = "recipeId"))
    @OrderColumn(name = "sortNo")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_steps", joinColumns = @JoinColumn(name = "recipeId"))
    @OrderColumn(name = "sortNo")
    private List<RecipeStep> steps = new ArrayList<>();

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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public Integer getTimeMinutes() { return timeMinutes; }
    public void setTimeMinutes(Integer timeMinutes) { this.timeMinutes = timeMinutes; }
    public Double getCarbG() { return carbG; }
    public void setCarbG(Double carbG) { this.carbG = carbG; }
    public Double getProteinG() { return proteinG; }
    public void setProteinG(Double proteinG) { this.proteinG = proteinG; }
    public Double getFatG() { return fatG; }
    public void setFatG(Double fatG) { this.fatG = fatG; }
    public Double getFiberG() { return fiberG; }
    public void setFiberG(Double fiberG) { this.fiberG = fiberG; }
    public String getDefaultReason() { return defaultReason; }
    public void setDefaultReason(String defaultReason) { this.defaultReason = defaultReason; }
    public String getValueText() { return valueText; }
    public void setValueText(String valueText) { this.valueText = valueText; }
    public String getOrganKeys() { return organKeys; }
    public void setOrganKeys(String organKeys) { this.organKeys = organKeys; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<RecipeIngredient> ingredients) { this.ingredients = ingredients; }
    public List<RecipeStep> getSteps() { return steps; }
    public void setSteps(List<RecipeStep> steps) { this.steps = steps; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

