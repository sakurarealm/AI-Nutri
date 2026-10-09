package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.DecisionActionType;
import com.xs.ai.aiheathagent.conversation.model.DecisionResult;
import com.xs.ai.aiheathagent.conversation.model.MessageChannel;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.conversation.expose.SystemMessageType;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.entity.DecisionConsumeRecord;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.DecisionConsumeRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProactiveTouchService {
    private static final String ONBOARDING_PREFIX = "新手引导｜";

    private final ConversationRecordRepository conversationRepository;
    private final DecisionConsumeRecordRepository consumeRepository;
    private final SystemMessageMetaService systemMessageMetaService;

    public ProactiveTouchService(
            ConversationRecordRepository conversationRepository,
            DecisionConsumeRecordRepository consumeRepository,
            SystemMessageMetaService systemMessageMetaService
    ) {
        this.conversationRepository = conversationRepository;
        this.consumeRepository = consumeRepository;
        this.systemMessageMetaService = systemMessageMetaService;
    }

    @Transactional
    public boolean execute(User user, DecisionResult decision, MessageContent content, MessageChannel channel) {
        return execute(user, decision, content, channel, null, null);
    }

    @Transactional
    public boolean execute(User user, DecisionResult decision, MessageContent content, MessageChannel channel, ExplanationCode explanationCode, String explanationArgs) {
        if (user == null || user.getId() == null || decision == null) {
            return false;
        }
        if (decision.actions() == null || !decision.actions().contains(DecisionActionType.PROACTIVE_INBOX)) {
            return false;
        }
        if (decision.validUntil() != null && LocalDateTime.now().isAfter(decision.validUntil())) {
            return false;
        }
        if (!consumeDecision(user.getId(), decision.decisionKey())) {
            return false;
        }
        MessageContent msg = content == null ? MessageContent.text("") : content;
        if (msg.text() == null || String.valueOf(msg.text()).trim().isEmpty()) {
            return false;
        }
        MessageChannel ch = channel == null ? MessageChannel.INBOX : channel;
        if (ch == MessageChannel.INBOX) {
            pushInboxMessage(user.getId(), msg.text(), decision.decisionKey(), explanationCode, explanationArgs);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean executeInboxTouch(User user, DecisionResult decision, MessageContent content) {
        return execute(user, decision, content, MessageChannel.INBOX);
    }

    @Transactional
    public boolean executeInboxTouch(User user, DecisionResult decision, MessageContent content, ExplanationCode explanationCode, String explanationArgs) {
        return execute(user, decision, content, MessageChannel.INBOX, explanationCode, explanationArgs);
    }

    @Transactional
    public boolean pushOnboardingIfNeeded(User user, String channel) {
        if (user == null || user.getId() == null) {
            return false;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return false;
        }
        Long userId = user.getId();
        String sessionId = inboxSessionId(userId);
        boolean exists = false;
        try {
            exists = conversationRepository.existsBySessionIdAndUserIdAndContentStartingWith(sessionId, userId, ONBOARDING_PREFIX);
        } catch (Exception ignored) {
        }
        if (exists) {
            return false;
        }
        if (!consumeDecision(userId, "onboarding:" + userId)) {
            return false;
        }

        String name = String.valueOf(user.getNickname() == null ? "" : user.getNickname()).trim();
        String prefix = name.isEmpty() ? "" : (name + "，");
        String ch = String.valueOf(channel == null ? "" : channel).trim();
        String via = ch.isEmpty() ? "" : ("（" + ch + "）");

        pushInboxMessage(userId, ONBOARDING_PREFIX + prefix + "欢迎来到 AI 营养师" + via + "。从现在起我就是你的专属营养师，会一直跟着你把目标做成。");
        pushInboxMessage(userId, ONBOARDING_PREFIX + "我能帮你做 4 件事：\n1）按你今天吃的东西做营养分析\n2）按目标给食谱/加餐/外卖点单建议\n3）帮你控糖、减脂、增肌或调理肠胃\n4）根据记录给你做复盘和下一步计划");
        pushInboxMessage(userId, ONBOARDING_PREFIX + "我们先把目标定清楚：你更想\n1减脂  2控糖  3增肌  4调理肠胃\n直接回我数字就行。");
        pushInboxMessage(userId, ONBOARDING_PREFIX + "你也可以直接把“身高体重 + 目标 + 今天吃了什么/准备吃什么”发我，我马上给你一条能执行的方案。");
        return true;
    }

    public MessageContent pushInboxMessage(Long userId, String message) {
        return pushInboxMessage(userId, message, "", null, null);
    }

    public MessageContent pushInboxMessage(Long userId, String message, String decisionKey, ExplanationCode explanationCode, String explanationArgs) {
        String text = String.valueOf(message == null ? "" : message).trim();
        if (userId == null || text.isEmpty()) {
            return MessageContent.text("");
        }
        ConversationRecord rec = new ConversationRecord();
        rec.setSessionId(inboxSessionId(userId));
        rec.setUserId(userId);
        rec.setMessageType(ConversationRecord.MessageType.ASSISTANT);
        rec.setContent(text);
        conversationRepository.save(rec);
        SystemMessageType type = inferType(decisionKey, text);
        String dk = String.valueOf(decisionKey == null ? "" : decisionKey).trim();
        String timeSlot = null;
        String subject = null;
        if (type == SystemMessageType.REMINDER) {
            timeSlot = extractJsonField(explanationArgs, "timeSlot");
            String itemKey = extractJsonField(explanationArgs, "itemKey");
            subject = deriveReminderSubject(itemKey);
        }
        systemMessageMetaService.attach(userId, rec.getId(), type, explanationCode, explanationArgs, dk, timeSlot, subject);
        return MessageContent.text(text);
    }

    private SystemMessageType inferType(String decisionKey, String text) {
        String dk = String.valueOf(decisionKey == null ? "" : decisionKey).trim();
        if (dk.startsWith("reminder:")) {
            return SystemMessageType.REMINDER;
        }
        if (dk.startsWith("summary:")) {
            return SystemMessageType.SUMMARY;
        }
        if (dk.startsWith("inquiry:")) {
            return SystemMessageType.PROACTIVE_INQUIRY;
        }
        String t = String.valueOf(text == null ? "" : text);
        if (t.startsWith(ONBOARDING_PREFIX)) {
            return SystemMessageType.SYSTEM_ACK;
        }
        return SystemMessageType.SYSTEM_ACK;
    }

    private static final Pattern JSON_FIELD = Pattern.compile("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\"([^\"]*)\"");

    private String extractJsonField(String json, String field) {
        String j = String.valueOf(json == null ? "" : json);
        if (j.isEmpty() || field == null || field.isEmpty()) return null;
        Matcher m = JSON_FIELD.matcher(j);
        while (m.find()) {
            if (field.equals(m.group(1))) {
                return m.group(2);
            }
        }
        return null;
    }

    private String deriveReminderSubject(String itemKey) {
        String k = String.valueOf(itemKey == null ? "" : itemKey).trim();
        if (k.isEmpty()) return "";
        if (k.startsWith("meal_breakfast")) return "早餐";
        if (k.startsWith("meal_lunch")) return "午餐";
        if (k.startsWith("meal_dinner")) return "晚餐";
        if (k.startsWith("medication_")) {
            String s = k.substring("medication_".length()).trim();
            return s.isEmpty() ? "用药" : s;
        }
        if (k.startsWith("supplement_")) {
            String s = k.substring("supplement_".length()).trim();
            return s.isEmpty() ? "补剂" : s;
        }
        if (k.startsWith("exercise_")) return "运动";
        return k;
    }

    private boolean consumeDecision(Long userId, String decisionKey) {
        String key = String.valueOf(decisionKey == null ? "" : decisionKey).trim();
        if (key.isEmpty()) {
            return false;
        }
        if (consumeRepository.existsByUserIdAndDecisionKey(userId, key)) {
            return false;
        }
        DecisionConsumeRecord rec = new DecisionConsumeRecord();
        rec.setUserId(userId);
        rec.setDecisionKey(key);
        consumeRepository.save(rec);
        return true;
    }

    private String inboxSessionId(Long userId) {
        return "inbox_" + String.valueOf(userId);
    }
}
