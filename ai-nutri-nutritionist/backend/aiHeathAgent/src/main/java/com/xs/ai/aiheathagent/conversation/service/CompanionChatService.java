package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.client.DeepSeekChatClient;
import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import com.xs.ai.aiheathagent.conversation.model.ConversationRole;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.prompt.Prompt;
import com.xs.ai.aiheathagent.conversation.prompt.PromptProvider;
import com.xs.ai.aiheathagent.conversation.prompt.PromptRole;
import com.xs.ai.aiheathagent.conversation.expose.SystemMessageType;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class CompanionChatService {
    private final ConversationRecordRepository conversationRepository;
    private final DeepSeekChatClient deepSeekChatClient;
    private final PromptProvider promptProvider;
    private final SystemMessageMetaService systemMessageMetaService;
    private static final String ONBOARDING_SESSION_PREFIX = "onb_";
    private static final String ONBOARDING_KICKOFF_TOKEN = "__ONBOARDING_KICKOFF__";

    public CompanionChatService(
            ConversationRecordRepository conversationRepository,
            DeepSeekChatClient deepSeekChatClient,
            PromptProvider promptProvider,
            SystemMessageMetaService systemMessageMetaService
    ) {
        this.conversationRepository = conversationRepository;
        this.deepSeekChatClient = deepSeekChatClient;
        this.promptProvider = promptProvider;
        this.systemMessageMetaService = systemMessageMetaService;
    }

    public record CompanionChatCommand(
            String sessionId,
            Long userId,
            List<ConversationMessage> history,
            MessageContent userMessage
    ) {
    }

    public record CompanionChatResult(
            MessageContent reply,
            Long userRecordId,
            Long assistantRecordId
    ) {
    }

    @Transactional
    public CompanionChatResult chat(CompanionChatCommand command) {
        String sid = String.valueOf(command == null ? "" : command.sessionId()).trim();
        Long userId = command == null ? null : command.userId();
        String userText = command == null || command.userMessage() == null ? "" : String.valueOf(command.userMessage().text()).trim();
        if (sid.isEmpty() || userId == null || userText.isEmpty()) {
            return new CompanionChatResult(MessageContent.text("我在呢。你想从饮食、运动还是作息开始？"), null, null);
        }

        boolean isKickoff = isOnboardingSession(sid) && ONBOARDING_KICKOFF_TOKEN.equals(userText);
        ConversationRecord userRecord = null;
        if (isKickoff) {
            userText = "开始新手引导";
        } else {
            userRecord = new ConversationRecord();
            userRecord.setSessionId(sid);
            userRecord.setUserId(userId);
            userRecord.setMessageType(ConversationRecord.MessageType.USER);
            userRecord.setContent(userText);
            conversationRepository.save(userRecord);
        }

        List<ConversationMessage> messages = new ArrayList<>();
        Prompt p = promptProvider.get(isOnboardingSession(sid) ? PromptRole.ONBOARDING_CHAT : PromptRole.COMPANION_CHAT);
        if (p != null) {
            String sys = String.valueOf(p.systemText() == null ? "" : p.systemText()).trim();
            if (!sys.isEmpty()) {
                messages.add(new ConversationMessage(ConversationRole.SYSTEM, MessageContent.text(sys), LocalDateTime.now()));
            }
        }
        if (command.history() != null && !command.history().isEmpty()) {
            messages.addAll(command.history());
        }
        messages.add(new ConversationMessage(ConversationRole.USER, MessageContent.text(userText), LocalDateTime.now()));

        String replyText;
        try {
            replyText = String.valueOf(deepSeekChatClient.complete(messages)).trim();
        } catch (Exception ex) {
            replyText = "我这边暂时连接不上智能服务。你把身高体重、目标和今天吃了什么发我，我先给你一版可执行建议。";
        }
        if (replyText.isEmpty()) {
            replyText = "我收到了。你想我先帮你分析今天吃的，还是先定目标？";
        }

        ConversationRecord aiRecord = new ConversationRecord();
        aiRecord.setSessionId(sid);
        aiRecord.setUserId(userId);
        aiRecord.setMessageType(ConversationRecord.MessageType.ASSISTANT);
        aiRecord.setContent(replyText);
        conversationRepository.save(aiRecord);
        systemMessageMetaService.attach(userId, aiRecord.getId(), SystemMessageType.CHAT, null, null);

        return new CompanionChatResult(MessageContent.text(replyText), userRecord == null ? null : userRecord.getId(), aiRecord.getId());
    }

    @Transactional
    public CompanionChatResult chatStream(CompanionChatCommand command, Consumer<String> onDelta) {
        String sid = String.valueOf(command == null ? "" : command.sessionId()).trim();
        Long userId = command == null ? null : command.userId();
        String userText = command == null || command.userMessage() == null ? "" : String.valueOf(command.userMessage().text()).trim();
        if (sid.isEmpty() || userId == null || userText.isEmpty()) {
            return new CompanionChatResult(MessageContent.text("我在呢。你想从饮食、运动还是作息开始？"), null, null);
        }

        boolean isKickoff = isOnboardingSession(sid) && ONBOARDING_KICKOFF_TOKEN.equals(userText);
        ConversationRecord userRecord = null;
        if (isKickoff) {
            userText = "开始新手引导";
        } else {
            userRecord = new ConversationRecord();
            userRecord.setSessionId(sid);
            userRecord.setUserId(userId);
            userRecord.setMessageType(ConversationRecord.MessageType.USER);
            userRecord.setContent(userText);
            conversationRepository.save(userRecord);
        }

        List<ConversationMessage> messages = new ArrayList<>();
        Prompt p = promptProvider.get(isOnboardingSession(sid) ? PromptRole.ONBOARDING_CHAT : PromptRole.COMPANION_CHAT);
        if (p != null) {
            String sys = String.valueOf(p.systemText() == null ? "" : p.systemText()).trim();
            if (!sys.isEmpty()) {
                messages.add(new ConversationMessage(ConversationRole.SYSTEM, MessageContent.text(sys), LocalDateTime.now()));
            }
        }
        if (command.history() != null && !command.history().isEmpty()) {
            messages.addAll(command.history());
        }
        messages.add(new ConversationMessage(ConversationRole.USER, MessageContent.text(userText), LocalDateTime.now()));

        ConversationRecord aiRecord = new ConversationRecord();
        aiRecord.setSessionId(sid);
        aiRecord.setUserId(userId);
        aiRecord.setMessageType(ConversationRecord.MessageType.ASSISTANT);
        aiRecord.setContent(" ");
        conversationRepository.save(aiRecord);

        String replyText;
        StringBuilder full = new StringBuilder();
        boolean[] emittedAny = new boolean[]{false};
        try {
            Consumer<String> deltaConsumer = onDelta == null ? (s) -> {} : onDelta;
            replyText = String.valueOf(deepSeekChatClient.streamComplete(messages, (delta) -> {
                String d = String.valueOf(delta == null ? "" : delta);
                if (d.isEmpty()) return;
                full.append(d);
                emittedAny[0] = true;
                deltaConsumer.accept(d);
            })).trim();
        } catch (Exception ex) {
            replyText = "";
        }

        if (replyText.isEmpty() && full.length() > 0) {
            replyText = full.toString().trim();
        }

        if (replyText.isEmpty()) {
            try {
                replyText = String.valueOf(deepSeekChatClient.complete(messages)).trim();
                if (!emittedAny[0] && onDelta != null && !replyText.isEmpty()) {
                    onDelta.accept(replyText);
                }
            } catch (Exception ex) {
                replyText = "我这边暂时连接不上智能服务。你把身高体重、目标和今天吃了什么发我，我先给你一版可执行建议。";
                if (!emittedAny[0] && onDelta != null) {
                    onDelta.accept(replyText);
                }
            }
        }
        if (replyText.isEmpty()) {
            replyText = "我收到了。你想我先帮你分析今天吃的，还是先定目标？";
            if (!emittedAny[0] && onDelta != null) {
                onDelta.accept(replyText);
            }
        }

        aiRecord.setContent(replyText);
        conversationRepository.save(aiRecord);
        systemMessageMetaService.attach(userId, aiRecord.getId(), SystemMessageType.CHAT, null, null);

        return new CompanionChatResult(MessageContent.text(replyText), userRecord == null ? null : userRecord.getId(), aiRecord.getId());
    }

    private boolean isOnboardingSession(String sessionId) {
        String sid = String.valueOf(sessionId == null ? "" : sessionId).trim();
        return !sid.isEmpty() && sid.startsWith(ONBOARDING_SESSION_PREFIX);
    }
}
