package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.client.DeepSeekChatClient;
import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import com.xs.ai.aiheathagent.conversation.model.ConversationRole;
import com.xs.ai.aiheathagent.conversation.model.DailyPlan;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.model.MessageContentType;
import com.xs.ai.aiheathagent.conversation.model.TimeSlot;
import com.xs.ai.aiheathagent.conversation.prompt.Prompt;
import com.xs.ai.aiheathagent.conversation.prompt.PromptProvider;
import com.xs.ai.aiheathagent.conversation.prompt.PromptRole;
import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReminderComposeService {
    private final PromptProvider promptProvider;
    private final DeepSeekChatClient deepSeekChatClient;
    private final boolean llmEnabled;

    public ReminderComposeService(
            PromptProvider promptProvider,
            DeepSeekChatClient deepSeekChatClient,
            @Value("${deepseek.reminder-compose.enabled:false}") boolean llmEnabled
    ) {
        this.promptProvider = promptProvider;
        this.deepSeekChatClient = deepSeekChatClient;
        this.llmEnabled = llmEnabled;
    }

    public MessageContent compose(User user, DailyPlan plan, ReminderIntentRecord intent) {
        String text = templateCompose(user, plan, intent);
        if (!llmEnabled) {
            return new MessageContent(MessageContentType.TEXT, text);
        }
        if (deepSeekChatClient == null || !deepSeekChatClient.isConfigured()) {
            return new MessageContent(MessageContentType.TEXT, text);
        }
        try {
            Prompt p = promptProvider.get(PromptRole.REMINDER_COMPOSE);
            String sys = p == null ? "" : String.valueOf(p.systemText() == null ? "" : p.systemText()).trim();
            String userMsg = buildUserMessage(user, plan, intent);
            List<ConversationMessage> messages = new ArrayList<>();
            if (!sys.isEmpty()) {
                messages.add(new ConversationMessage(ConversationRole.SYSTEM, MessageContent.text(sys), LocalDateTime.now()));
            }
            messages.add(new ConversationMessage(ConversationRole.USER, MessageContent.text(userMsg), LocalDateTime.now()));
            String out = String.valueOf(deepSeekChatClient.lightComplete(messages)).trim();
            if (!out.isEmpty()) {
                text = out;
            }
        } catch (Exception ignored) {
        }
        return new MessageContent(MessageContentType.TEXT, text);
    }

    private String buildUserMessage(User user, DailyPlan plan, ReminderIntentRecord intent) {
        String name = user == null ? "" : String.valueOf(user.getNickname() == null ? "" : user.getNickname()).trim();
        String prefix = name.isEmpty() ? "" : (name + "，");
        String slot = intent == null ? "" : String.valueOf(intent.getTimeSlot() == null ? "" : intent.getTimeSlot());
        String itemKey = intent == null ? "" : String.valueOf(intent.getItemKey() == null ? "" : intent.getItemKey());
        String date = plan == null || plan.planDate() == null ? "" : plan.planDate().toString();
        return prefix + "请基于以下计划项写一句提醒：date=" + date + ", timeSlot=" + slot + ", itemKey=" + itemKey + "。要求：一句话或两句话，亲切，直接可执行，不要问诊。";
    }

    private String templateCompose(User user, DailyPlan plan, ReminderIntentRecord intent) {
        String name = user == null ? "" : String.valueOf(user.getNickname() == null ? "" : user.getNickname()).trim();
        String prefix = name.isEmpty() ? "" : (name + "，");
        TimeSlot slot = null;
        try {
            slot = intent == null ? null : TimeSlot.valueOf(String.valueOf(intent.getTimeSlot()));
        } catch (Exception ignored) {
        }
        if (slot == TimeSlot.MORNING) {
            return prefix + "到早餐时间啦，按今天的结构吃一顿：蛋白 + 高纤维主食 + 蔬果。";
        }
        if (slot == TimeSlot.NOON) {
            return prefix + "午饭记得先菜再肉最后主食，吃完走一走更舒服。";
        }
        if (slot == TimeSlot.EVENING) {
            return prefix + "晚餐尽量清淡七分饱，别太晚吃哦。";
        }
        if (slot == TimeSlot.AFTERNOON) {
            return prefix + "下午补一口水或无糖茶，起来活动两分钟。";
        }
        if (slot == TimeSlot.NIGHT) {
            return prefix + "晚上收个尾：回想一下今天吃了什么，有需要我帮你复盘。";
        }
        return prefix + "我来提醒你一下：按计划做一小步就很好。";
    }
}
