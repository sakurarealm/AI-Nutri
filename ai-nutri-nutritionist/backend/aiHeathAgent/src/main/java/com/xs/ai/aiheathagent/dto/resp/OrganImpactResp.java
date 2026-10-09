package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganImpactResp {
    private Long memberId;
    private String date;
    private OrganImpactProfile profile;
    private List<OrganImpactItem> impacts;
    private Map<String, OrganImpactItem> impactByKey;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public OrganImpactProfile getProfile() { return profile; }
    public void setProfile(OrganImpactProfile profile) { this.profile = profile; }
    public List<OrganImpactItem> getImpacts() { return impacts; }
    public void setImpacts(List<OrganImpactItem> impacts) { this.impacts = impacts; }
    public Map<String, OrganImpactItem> getImpactByKey() { return impactByKey; }
    public void setImpactByKey(Map<String, OrganImpactItem> impactByKey) { this.impactByKey = impactByKey; }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrganImpactProfile {
        private String date;
        private int foodsCount;
        private int calorieConsumed;
        private int calorieTarget;
        private Map<String, Integer> macroPercent;
        private Map<String, Double> totals;
        private double fiberG;
        private int sugarHits;
        private int saltyHits;
        private int vegHits;
        private int omega3Score;
        private int snackRisk;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public int getFoodsCount() { return foodsCount; }
        public void setFoodsCount(int foodsCount) { this.foodsCount = foodsCount; }
        public int getCalorieConsumed() { return calorieConsumed; }
        public void setCalorieConsumed(int calorieConsumed) { this.calorieConsumed = calorieConsumed; }
        public int getCalorieTarget() { return calorieTarget; }
        public void setCalorieTarget(int calorieTarget) { this.calorieTarget = calorieTarget; }
        public Map<String, Integer> getMacroPercent() { return macroPercent; }
        public void setMacroPercent(Map<String, Integer> macroPercent) { this.macroPercent = macroPercent; }
        public Map<String, Double> getTotals() { return totals; }
        public void setTotals(Map<String, Double> totals) { this.totals = totals; }
        public double getFiberG() { return fiberG; }
        public void setFiberG(double fiberG) { this.fiberG = fiberG; }
        public int getSugarHits() { return sugarHits; }
        public void setSugarHits(int sugarHits) { this.sugarHits = sugarHits; }
        public int getSaltyHits() { return saltyHits; }
        public void setSaltyHits(int saltyHits) { this.saltyHits = saltyHits; }
        public int getVegHits() { return vegHits; }
        public void setVegHits(int vegHits) { this.vegHits = vegHits; }
        public int getOmega3Score() { return omega3Score; }
        public void setOmega3Score(int omega3Score) { this.omega3Score = omega3Score; }
        public int getSnackRisk() { return snackRisk; }
        public void setSnackRisk(int snackRisk) { this.snackRisk = snackRisk; }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrganImpactItem {
        private String key;
        private String name;
        private String emoji;
        private int delta;
        private String deltaText;
        private String level;
        private String reason;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmoji() { return emoji; }
        public void setEmoji(String emoji) { this.emoji = emoji; }
        public int getDelta() { return delta; }
        public void setDelta(int delta) { this.delta = delta; }
        public String getDeltaText() { return deltaText; }
        public void setDeltaText(String deltaText) { this.deltaText = deltaText; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}

