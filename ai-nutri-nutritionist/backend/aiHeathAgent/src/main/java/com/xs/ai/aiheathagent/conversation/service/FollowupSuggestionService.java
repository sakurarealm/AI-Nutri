package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FollowupSuggestionService {
    private final ConversationHistoryQueryService historyQueryService;

    public FollowupSuggestionService(ConversationHistoryQueryService historyQueryService) {
        this.historyQueryService = historyQueryService;
    }

    public List<String> suggest(String sessionId, Long userId) {
        return suggest(sessionId, userId, "", "");
    }

    public List<String> suggest(String sessionId, Long userId, String lastUserText, String lastAssistantText) {
        String sid = String.valueOf(sessionId == null ? "" : sessionId).trim();
        if (sid.isEmpty() || userId == null) {
            return List.of();
        }
        String lastUser = safeText(lastUserText);
        String lastAssistant = safeText(lastAssistantText);
        if (lastUser.isEmpty() && lastAssistant.isEmpty()) {
            List<ConversationMessage> base = historyQueryService.loadRecent(sid, userId);
            if (base.isEmpty()) {
                return defaults();
            }
            for (int i = base.size() - 1; i >= 0; i--) {
                ConversationMessage m = base.get(i);
                if (m == null || m.role() == null || m.content() == null) continue;
                String text = safeText(m.content().text());
                if (text.isEmpty()) continue;
                if (lastUser.isEmpty() && m.role() == com.xs.ai.aiheathagent.conversation.model.ConversationRole.USER) {
                    lastUser = text;
                } else if (lastAssistant.isEmpty() && m.role() == com.xs.ai.aiheathagent.conversation.model.ConversationRole.ASSISTANT) {
                    lastAssistant = text;
                }
                if (!lastUser.isEmpty() && !lastAssistant.isEmpty()) break;
            }
        }

        List<String> out = build(lastUser, lastAssistant);
        if (out.size() >= 3) return out;
        Set<String> dedup = new LinkedHashSet<>(out);
        for (String s : defaults()) {
            if (dedup.size() >= 3) break;
            String v = clip(safeText(s));
            if (!v.isEmpty()) dedup.add(v);
        }
        return new ArrayList<>(dedup);
    }

    private List<String> build(String lastUser, String lastAssistant) {
        String u = safeText(lastUser);
        String a = safeText(lastAssistant);
        Set<String> dedup = new LinkedHashSet<>();

        if (containsAny(u, "总结", "复盘", "回顾", "今天吃", "吃了", "热量", "卡路里", "碳水", "蛋白", "脂肪", "外卖", "早餐", "午餐", "晚餐", "加餐")) {
            add(dedup, "帮我分析今天吃的");
            add(dedup, "估算这顿的热量");
            add(dedup, "推荐下一顿怎么吃");
        }

        if (containsAny(u, "减脂", "减重", "瘦", "体脂", "控脂", "热量缺口")) {
            add(dedup, "给我一个减脂计划");
            add(dedup, "推荐低卡晚餐");
            add(dedup, "外卖怎么点更稳");
        }

        if (containsAny(u, "增肌", "增重", "长肌肉")) {
            add(dedup, "增肌怎么吃更省心");
            add(dedup, "蛋白质要吃多少");
            add(dedup, "训练日怎么配餐");
        }

        if (containsAny(u, "控糖", "血糖", "糖尿病", "糖前", "胰岛素", "餐后")) {
            add(dedup, "控糖一日三餐怎么安排");
            add(dedup, "这顿怎么吃不飙糖");
            add(dedup, "给我3个低GI选择");
        }

        if (containsAny(u, "菜谱", "食谱", "做法", "怎么做", "吃什么", "推荐", "做饭")) {
            add(dedup, "推荐3个家常低脂菜");
            add(dedup, "按目标给我一周食谱");
            add(dedup, "按冰箱食材推荐");
        }

        if (containsAny(u, "提醒", "闹钟", "定时", "别忘", "不要忘")) {
            add(dedup, "帮我设置提醒");
            add(dedup, "查看我的提醒");
            add(dedup, "关闭某条提醒");
        }

        if (containsAny(u, "身高", "体重", "年龄", "性别", "资料", "档案", "信息", "怎么填", "在哪填", "目标")) {
            add(dedup, "身高体重怎么填");
            add(dedup, "我想减脂，怎么安排");
            add(dedup, "帮我把目标定清楚");
        }

        if (containsAny(a, "身高", "体重", "年龄", "目标")) {
            add(dedup, "身高体重怎么填");
            add(dedup, "我的目标是…");
        }

        if (dedup.isEmpty()) {
            add(dedup, "我今天吃了这些…");
            add(dedup, "我想减脂，怎么安排");
            add(dedup, "推荐一份晚餐");
        }

        List<String> out = new ArrayList<>(dedup);
        if (out.size() > 3) out = out.subList(0, 3);
        return out;
    }

    private void add(Set<String> out, String v) {
        if (out == null) return;
        String s = clip(safeText(v));
        if (s.isEmpty()) return;
        out.add(s);
    }

    private boolean containsAny(String text, String... needles) {
        String t = safeText(text);
        if (t.isEmpty() || needles == null || needles.length == 0) return false;
        for (String n : needles) {
            String k = safeText(n);
            if (k.isEmpty()) continue;
            if (t.contains(k)) return true;
        }
        return false;
    }

    private String safeText(String s) {
        String t = String.valueOf(s == null ? "" : s).trim();
        if (t.isEmpty()) return "";
        return t.replaceAll("\\s+", " ").trim();
    }

    private String clip(String s) {
        String t = safeText(s);
        if (t.isEmpty()) return "";
        t = t.replaceAll("^[\\s\\-\\*\\d\\.、)）]+", "").trim();
        if (t.length() > 24) t = t.substring(0, 24);
        return t;
    }

    private List<String> defaults() {
        return List.of("帮我分析今天吃的", "我想减脂，怎么安排", "身高体重怎么填");
    }
}
