package com.xs.ai.aiheathagent.vision;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xs.ai.aiheathagent.conversation.client.DeepSeekChatClient;
import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import com.xs.ai.aiheathagent.conversation.model.ConversationRole;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.prompt.Prompt;
import com.xs.ai.aiheathagent.conversation.prompt.PromptProvider;
import com.xs.ai.aiheathagent.conversation.prompt.PromptRole;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MealRecognitionAiService {
    private final DeepSeekChatClient deepSeekChatClient;
    private final PromptProvider promptProvider;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

    public MealRecognitionAiService(DeepSeekChatClient deepSeekChatClient, PromptProvider promptProvider) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.promptProvider = promptProvider;
    }

    public MealRecognitionAiOutput analyze(MealRecognitionAiInput input) {
        MealRecognitionAiInput in = input == null ? new MealRecognitionAiInput() : input;
        if (deepSeekChatClient == null || !deepSeekChatClient.isConfigured()) {
            return fallback(in);
        }

        Prompt p = promptProvider == null ? null : promptProvider.get(PromptRole.FOOD_RECOGNITION);
        String system = p == null ? "" : String.valueOf(p.systemText() == null ? "" : p.systemText()).trim();

        String user = buildUserJson(in);
        List<ConversationMessage> messages = List.of(
                new ConversationMessage(ConversationRole.SYSTEM, MessageContent.text(system), LocalDateTime.now()),
                new ConversationMessage(ConversationRole.USER, MessageContent.text(user), LocalDateTime.now())
        );

        String out = deepSeekChatClient.lightComplete(messages);
        MealRecognitionAiOutput parsed = parseJson(out);
        if (parsed == null) {
            return fallback(in);
        }
        normalize(parsed);
        if (isEmpty(parsed.getSummary()) && (parsed.getItems() == null || parsed.getItems().isEmpty())) {
            return fallback(in);
        }
        return parsed;
    }

    private void normalize(MealRecognitionAiOutput o) {
        if (o == null) return;
        o.setSummary(String.valueOf(o.getSummary() == null ? "" : o.getSummary()).trim());
        o.setNutritionText(String.valueOf(o.getNutritionText() == null ? "" : o.getNutritionText()).trim());
        o.setNotesText(String.valueOf(o.getNotesText() == null ? "" : o.getNotesText()).trim());
        o.setImpactText(String.valueOf(o.getImpactText() == null ? "" : o.getImpactText()).trim());
        List<MealItem> items = o.getItems() == null ? new ArrayList<>() : o.getItems();
        List<MealItem> cleaned = new ArrayList<>();
        for (MealItem it : items) {
            if (it == null) continue;
            String name = String.valueOf(it.getName() == null ? "" : it.getName()).trim();
            if (name.isEmpty()) continue;
            MealItem m = new MealItem();
            m.setName(name);
            m.setPortion(String.valueOf(it.getPortion() == null ? "" : it.getPortion()).trim());
            int cal = it.getCalories() == null ? 0 : Math.max(0, it.getCalories());
            m.setCalories(cal);
            cleaned.add(m);
            if (cleaned.size() >= 3) break;
        }
        o.setItems(cleaned);
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String buildUserJson(MealRecognitionAiInput in) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("top1", String.valueOf(in.getTop1Name() == null ? "" : in.getTop1Name()).trim());
        payload.put("top1Probability", in.getTop1Probability() == null ? 0.0 : in.getTop1Probability());
        payload.put("top1Calorie", in.getTop1Calorie() == null ? 0.0 : in.getTop1Calorie());
        payload.put("top1Carb", in.getTop1Carb() == null ? 0.0 : in.getTop1Carb());
        payload.put("top1Protein", in.getTop1Protein() == null ? 0.0 : in.getTop1Protein());
        payload.put("top1Fat", in.getTop1Fat() == null ? 0.0 : in.getTop1Fat());
        List<Map<String, Object>> candidates = new ArrayList<>();
        List<FoodCandidate> list = in.getCandidates() == null ? List.of() : in.getCandidates();
        for (FoodCandidate c : list) {
            if (c == null) continue;
            String name = String.valueOf(c.getName() == null ? "" : c.getName()).trim();
            if (name.isEmpty()) continue;
            candidates.add(Map.of(
                    "name", name,
                    "probability", c.getProbability() == null ? 0.0 : c.getProbability(),
                    "calorie", c.getCalorie() == null ? 0.0 : c.getCalorie()
            ));
            if (candidates.size() >= 3) break;
        }
        payload.put("candidates", candidates);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }

    private MealRecognitionAiOutput parseJson(String txt) {
        String t = String.valueOf(txt == null ? "" : txt).trim();
        if (t.isEmpty()) return null;
        int l = t.indexOf('{');
        int r = t.lastIndexOf('}');
        if (l >= 0 && r > l) {
            String body = t.substring(l, r + 1);
            try {
                return objectMapper.readValue(body, MealRecognitionAiOutput.class);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private MealRecognitionAiOutput fallback(MealRecognitionAiInput in) {
        String name = String.valueOf(in == null ? "" : in.getTop1Name()).trim();
        MealRecognitionAiOutput o = new MealRecognitionAiOutput();
        o.setSummary(name.isEmpty() ? "识别完成，可查看这餐的要点。" : ("识别到：" + name + "。这餐先关注份量与油盐。"));
        o.setNutritionText("营养构成：以蛋白质、蔬菜和主食的比例为主，结合份量控制。");
        o.setNotesText("外食尽量少油少盐；如果偏咸，搭配清淡蔬菜和水。");
        o.setImpactText("记录到营养册后，可在当天汇总里看到热量与三大营养素变化。");
        List<MealItem> items = new ArrayList<>();
        List<FoodCandidate> cands = in == null || in.getCandidates() == null ? List.of() : in.getCandidates();
        for (FoodCandidate c : cands) {
            if (c == null) continue;
            String n = String.valueOf(c.getName() == null ? "" : c.getName()).trim();
            if (n.isEmpty()) continue;
            MealItem mi = new MealItem();
            mi.setName(n);
            mi.setPortion("一份");
            int cal = c.getCalorie() == null ? 0 : Math.max(0, (int) Math.round(c.getCalorie()));
            mi.setCalories(cal);
            items.add(mi);
            if (items.size() >= 3) break;
        }
        o.setItems(items);
        return o;
    }

    @Data
    public static class MealRecognitionAiInput {
        private String top1Name;
        private Double top1Probability;
        private Double top1Calorie;
        private Double top1Carb;
        private Double top1Protein;
        private Double top1Fat;
        private List<FoodCandidate> candidates;
    }

    @Data
    public static class MealRecognitionAiOutput {
        private String summary;
        private String nutritionText;
        private String notesText;
        private String impactText;
        private List<MealItem> items;
    }

    @Data
    public static class MealItem {
        private String name;
        private String portion;
        private Integer calories;
    }
}
