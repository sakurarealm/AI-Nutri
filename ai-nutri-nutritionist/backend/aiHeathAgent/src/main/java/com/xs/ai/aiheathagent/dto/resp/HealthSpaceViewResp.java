package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HealthSpaceViewResp {
    private Integer viewIndex;
    private Integer completionRate;
    private String completionHint;
    private Integer streakDays;
    private Integer warmScore;
    private Pulse pulse;
    private String pulseSubtitle;
    private String barLeft;
    private String barRight;
    private List<TimelineItem> timeline;
    private List<Achievement> achievements;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Pulse {
        private Integer reached;
        private Integer stepsAvg;
        private Integer lightMealRate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimelineItem {
        private String id;
        private String time;
        private String badge;
        private String title;
        private String desc;
        private List<Kv> kv;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Kv {
        private String k;
        private String v;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Achievement {
        private String id;
        private String icon;
        private String text;
    }
}

