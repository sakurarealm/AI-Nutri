package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class RecipeIngredient {
    @Column(nullable = false, length = 128)
    private String name = "";

    @Column(nullable = false, length = 64)
    private String amount = "";

    public RecipeIngredient() {}

    public RecipeIngredient(String name, String amount) {
        this.name = String.valueOf(name == null ? "" : name);
        this.amount = String.valueOf(amount == null ? "" : amount);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
}

