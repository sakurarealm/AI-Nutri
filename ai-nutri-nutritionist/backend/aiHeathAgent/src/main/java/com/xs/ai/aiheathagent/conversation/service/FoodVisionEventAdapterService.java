package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.InputModality;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.model.UserEvent;
import com.xs.ai.aiheathagent.conversation.model.UserEventTopic;
import com.xs.ai.aiheathagent.conversation.model.UserEventType;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import com.xs.ai.aiheathagent.vision.FoodCandidate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class FoodVisionEventAdapterService {
    private final UserEventRecordRepository userEventRecordRepository;

    public FoodVisionEventAdapterService(UserEventRecordRepository userEventRecordRepository) {
        this.userEventRecordRepository = userEventRecordRepository;
    }

    @Transactional
    public UserEvent adaptAndPersist(Long userId, String sessionId, List<FoodCandidate> candidates) {
        Long uid = userId == null ? 1L : userId;
        String sid = String.valueOf(sessionId == null ? "" : sessionId).trim();
        if (sid.isEmpty()) {
            sid = "vision_" + uid;
        }
        LocalDateTime at = LocalDateTime.now();
        Set<UserEventTopic> topics = mapTopics(candidates);
        double confidence = estimateConfidence(candidates);
        String text = buildEventText(candidates);

        UserEventRecord rec = new UserEventRecord();
        rec.setUserId(uid);
        rec.setSessionId(sid);
        rec.setOccurredAt(at);
        rec.setModality(InputModality.IMAGE.name());
        rec.setEventType(UserEventType.MEAL.name());
        rec.setTopics(topicsToString(topics));
        rec.setConfidence(confidence);
        rec.setSuggestOpenCamera(false);
        rec.setSuggestWriteNutrition(true);
        rec.setContentText(text);
        userEventRecordRepository.save(rec);

        return new UserEvent(
                rec.getId(),
                uid,
                sid,
                at,
                UserEventType.MEAL,
                topics,
                confidence,
                false,
                true,
                MessageContent.text(text),
                InputModality.IMAGE
        );
    }

    private Set<UserEventTopic> mapTopics(List<FoodCandidate> candidates) {
        EnumSet<UserEventTopic> topics = EnumSet.noneOf(UserEventTopic.class);
        if (candidates == null || candidates.isEmpty()) {
            return topics;
        }
        for (int i = 0; i < Math.min(3, candidates.size()); i++) {
            FoodCandidate c = candidates.get(i);
            String name = c == null ? "" : String.valueOf(c.getName() == null ? "" : c.getName()).trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.matches(".*(炸|油条|薯条|炸鸡|炸.*|煎|烧烤|烤串).*")) {
                topics.add(UserEventTopic.FAT_LOSS);
            }
            if (name.matches(".*(奶茶|蛋糕|甜|糖|可乐|冰淇淋|巧克力|布丁).*")) {
                topics.add(UserEventTopic.CONTROL_SUGAR);
            }
        }
        return topics;
    }

    private String topicsToString(Set<UserEventTopic> topics) {
        if (topics == null || topics.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (UserEventTopic t : topics) {
            if (t == null) continue;
            if (!sb.isEmpty()) sb.append(",");
            sb.append(t.name());
        }
        return sb.toString();
    }

    private double estimateConfidence(List<FoodCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return 0.4;
        }
        double sum = 0.0;
        int n = 0;
        for (int i = 0; i < Math.min(3, candidates.size()); i++) {
            FoodCandidate c = candidates.get(i);
            if (c == null) continue;
            sum += Math.max(0.0, Math.min(1.0, c.getProbability()));
            n++;
        }
        if (n == 0) {
            return 0.4;
        }
        return sum / n;
    }

    private String buildEventText(List<FoodCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("拍照识别：");
        if (candidates == null || candidates.isEmpty()) {
            sb.append("未识别出菜品");
            return sb.toString();
        }
        int n = Math.min(5, candidates.size());
        for (int i = 0; i < n; i++) {
            FoodCandidate c = candidates.get(i);
            if (c == null) continue;
            String name = String.valueOf(c.getName() == null ? "" : c.getName()).trim();
            if (name.isEmpty()) continue;
            if (sb.charAt(sb.length() - 1) != '：') sb.append("，");
            sb.append(name);
        }
        return sb.toString();
    }
}

