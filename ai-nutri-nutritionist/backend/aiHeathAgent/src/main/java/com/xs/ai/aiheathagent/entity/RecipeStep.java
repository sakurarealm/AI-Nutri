package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class RecipeStep {
    @Column(nullable = false, length = 128)
    private String title = "";

    @Column(nullable = false, length = 1024)
    private String description = "";

    @Column(nullable = false)
    private Integer timeMinutes = 0;

    public RecipeStep() {}

    public RecipeStep(String title, String description, Integer timeMinutes) {
        this.title = String.valueOf(title == null ? "" : title);
        this.description = String.valueOf(description == null ? "" : description);
        this.timeMinutes = timeMinutes == null ? 0 : Math.max(0, timeMinutes);
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getTimeMinutes() { return timeMinutes; }
    public void setTimeMinutes(Integer timeMinutes) { this.timeMinutes = timeMinutes; }
}

