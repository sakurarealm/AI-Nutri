package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.ConversationInput;
import com.xs.ai.aiheathagent.conversation.model.InputModality;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.model.UserEvent;
import com.xs.ai.aiheathagent.conversation.model.UserEventTopic;
import com.xs.ai.aiheathagent.conversation.model.UserEventType;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RuleBasedEventDetectService implements EventDetectService {
    private final UserEventRecordRepository userEventRecordRepository;
    private final FoodVisionEventAdapterService foodVisionEventAdapterService;

    public RuleBasedEventDetectService(UserEventRecordRepository userEventRecordRepository, FoodVisionEventAdapterService foodVisionEventAdapterService) {
        this.userEventRecordRepository = userEventRecordRepository;
        this.foodVisionEventAdapterService = foodVisionEventAdapterService;
    }

    @Override
    @Transactional
    public UserEvent detectAndPersist(ConversationInput input) {
        try {
            ConversationInput safe = input == null ? ConversationInput.text("", 0L, "") : input;
            Long userId = safe.userId() == null ? 0L : safe.userId();
            String sessionId = String.valueOf(safe.sessionId() == null ? "" : safe.sessionId()).trim();
            InputModality modality = safe.modality() == null ? InputModality.TEXT : safe.modality();
            MessageContent content = safe.content() == null ? MessageContent.text("") : safe.content();
            String text = String.valueOf(content.text() == null ? "" : content.text()).trim();
            LocalDateTime at = safe.receivedAt() == null ? LocalDateTime.now() : safe.receivedAt();

            if (modality == InputModality.IMAGE) {
                return foodVisionEventAdapterService.adaptAndPersist(userId, sessionId, safe.foodCandidates());
            }

            Detection d = detect(text);

            UserEventRecord rec = new UserEventRecord();
            rec.setUserId(userId);
            rec.setSessionId(sessionId.isEmpty() ? "default" : sessionId);
            rec.setOccurredAt(at);
            rec.setModality(modality.name());
            rec.setEventType(d.type().name());
            rec.setTopics(d.topics().stream().map(Enum::name).collect(Collectors.joining(",")));
            rec.setConfidence(d.confidence());
            rec.setSuggestOpenCamera(d.suggestOpenCamera());
            rec.setSuggestWriteNutrition(d.suggestWriteNutrition());
            rec.setContentText(text);
            userEventRecordRepository.save(rec);

            return new UserEvent(
                    rec.getId(),
                    userId,
                    rec.getSessionId(),
                    rec.getOccurredAt(),
                    d.type(),
                    d.topics(),
                    d.confidence(),
                    d.suggestOpenCamera(),
                    d.suggestWriteNutrition(),
                    content,
                    modality
            );
        } catch (Exception ex) {
            return persistUnknown(input);
        }
    }

    private UserEvent persistUnknown(ConversationInput input) {
        ConversationInput safe = input == null ? ConversationInput.text("", 0L, "") : input;
        Long userId = safe.userId() == null ? 0L : safe.userId();
        String sessionId = String.valueOf(safe.sessionId() == null ? "" : safe.sessionId()).trim();
        InputModality modality = safe.modality() == null ? InputModality.TEXT : safe.modality();
        MessageContent content = safe.content() == null ? MessageContent.text("") : safe.content();
        String text = String.valueOf(content.text() == null ? "" : content.text()).trim();
        LocalDateTime at = safe.receivedAt() == null ? LocalDateTime.now() : safe.receivedAt();

        UserEventRecord rec = new UserEventRecord();
        rec.setUserId(userId);
        rec.setSessionId(sessionId.isEmpty() ? "default" : sessionId);
        rec.setOccurredAt(at);
        rec.setModality(modality.name());
        rec.setEventType(UserEventType.UNKNOWN.name());
        rec.setTopics("");
        rec.setConfidence(0.0);
        rec.setSuggestOpenCamera(false);
        rec.setSuggestWriteNutrition(false);
        rec.setContentText(text);
        userEventRecordRepository.save(rec);

        return new UserEvent(
                rec.getId(),
                userId,
                rec.getSessionId(),
                rec.getOccurredAt(),
                UserEventType.UNKNOWN,
                Set.of(),
                0.0,
                false,
                false,
                content,
                modality
        );
    }

    private record Detection(
            UserEventType type,
            Set<UserEventTopic> topics,
            double confidence,
            boolean suggestOpenCamera,
            boolean suggestWriteNutrition
    ) {
    }

    private Detection detect(String text) {
        String t = String.valueOf(text == null ? "" : text).trim();
        if (t.isEmpty()) {
            return new Detection(UserEventType.UNKNOWN, Set.of(), 0.2, false, false);
        }

        EnumSet<UserEventTopic> topics = EnumSet.noneOf(UserEventTopic.class);
        if (t.matches(".*(减脂|减肥|瘦|体脂|热量|卡路里).*")) topics.add(UserEventTopic.FAT_LOSS);
        if (t.matches(".*(控糖|血糖|糖尿病|胰岛素).*")) topics.add(UserEventTopic.CONTROL_SUGAR);
        if (t.matches(".*(增肌|健身|力量|蛋白|肌肉).*")) topics.add(UserEventTopic.MUSCLE_GAIN);
        if (t.matches(".*(胃|肠|拉肚子|腹泻|便秘|胃疼|胀气|反酸).*")) topics.add(UserEventTopic.DIGESTION);
        if (t.matches(".*(睡|失眠|熬夜|困|精神).*")) topics.add(UserEventTopic.SLEEP);

        boolean mealTopic = t.matches(".*(早餐|午餐|晚餐|夜宵|加餐|外卖|点单|吃什么|低卡|高蛋白|热量|卡路里|碳水|蛋白|脂肪|菜谱|食谱|做饭|下厨|做菜).*");
        boolean ate = t.matches(".*(我(刚|已经|又)?吃(了|的)?|刚吃|吃完|吃过|今天.*吃|这(顿|餐)|早餐吃|午餐吃|晚餐吃|夜宵吃|加餐吃|我点(了)?外卖|点了外卖|我做(了)?|自己做|下厨|炒了|煮了|烧了).*");
        boolean medicine = t.matches(".*(吃药|用药|药|胰岛素|剂量|片|胶囊).*");
        boolean supplement = t.matches(".*(鱼油|维生素|钙|锌|益生菌|dha|omega|辅酶|叶酸|铁剂).*");
        boolean goal = t.matches(".*(目标|想要|计划|安排|坚持|打卡|减脂|控糖|增肌).*");

        boolean suggestCamera = t.contains("拍") || t.contains("照片") || t.contains("图片") || t.contains("[图片]") || t.contains("识别") || t.contains("扫一扫");
        boolean suggestWrite = t.contains("记录") || t.contains("打卡") || t.contains("写入") || t.contains("录入") || t.contains("存到") || t.contains("记一下") || t.contains("帮我记");
        boolean mealRecord = ate || t.matches(".*(早餐|午餐|晚餐|夜宵|加餐)\\s*[:：].*") || (suggestWrite && mealTopic);

        if (medicine || supplement) {
            if (supplement) topics.add(UserEventTopic.SUPPLEMENT);
            return new Detection(UserEventType.MEDICINE, topics, 0.85, false, false);
        }
        if (mealRecord) {
            return new Detection(UserEventType.MEAL, topics, 0.8, suggestCamera, true);
        }
        if (goal) {
            return new Detection(UserEventType.GOAL, topics, 0.75, false, false);
        }
        return new Detection(UserEventType.CHAT, topics, 0.6, suggestCamera, false);
    }
}
