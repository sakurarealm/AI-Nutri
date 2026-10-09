package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganDetailResp {
    private Long memberId;
    private String date;
    private String organKey;

    private Organ organ;
    private List<Integer> history;
    private List<OrganTab> organTabs;

    private String dietImpactText;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Organ {
        private String key;
        private String name;
        private String emoji;
        private String intro;
        private int score;
        private String summary;
        private List<Metric> metrics;
        private List<Suggestion> suggestions;
        private List<Food> recommendations;
        private String dailyTip;
        private String aiPlan;
        private List<String> achievements;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Metric {
        private String name;
        private String value;
        private String unit;
        private int percentage;
        private String status;
        private String detail;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Suggestion {
        private String type;
        private String title;
        private String description;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Food {
        private String name;
        private String img;
        private String note;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrganTab {
        private String key;
        private String name;
        private String emoji;
        private int score;
    }
}

