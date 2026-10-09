package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import com.xs.ai.aiheathagent.conversation.model.ConversationRole;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationHistoryQueryService {
    private final ConversationRecordRepository conversationRepository;
    private final int maxTurns;
    private final int maxMessageChars;

    public ConversationHistoryQueryService(
            ConversationRecordRepository conversationRepository,
            @Value("${deepseek.max-turns:8}") int maxTurns,
            @Value("${deepseek.max-message-chars:520}") int maxMessageChars
    ) {
        this.conversationRepository = conversationRepository;
        this.maxTurns = maxTurns;
        this.maxMessageChars = maxMessageChars;
    }

    public List<ConversationMessage> loadRecent(String sessionId, Long userId) {
        String sid = String.valueOf(sessionId == null ? "" : sessionId).trim();
        if (sid.isEmpty() || userId == null) {
            return List.of();
        }
        int turns = Math.max(1, maxTurns);
        int pageSize = Math.max(2, turns * 2);
        List<ConversationRecord> history = new ArrayList<>(conversationRepository
                .findBySessionIdAndUserIdOrderByIdDesc(sid, userId, PageRequest.of(0, pageSize))
                .getContent());
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        Collections.reverse(history);
        List<ConversationMessage> out = new ArrayList<>();
        ConversationMessage pendingUser = null;
        for (int i = 0; i < history.size(); i++) {
            ConversationRecord r = history.get(i);
            if (r == null) continue;
            String raw = String.valueOf(r.getContent() == null ? "" : r.getContent());
            String content = raw.replaceAll("\\s+", " ").trim();
            if (content.isEmpty()) continue;
            int limit = Math.max(32, maxMessageChars);
            if (content.length() > limit) content = content.substring(0, limit);

            boolean isUser = r.getMessageType() == ConversationRecord.MessageType.USER;
            if (isUser) {
                pendingUser = new ConversationMessage(ConversationRole.USER, MessageContent.text(content), r.getTimestamp());
                continue;
            }
            ConversationMessage assistant = new ConversationMessage(ConversationRole.ASSISTANT, MessageContent.text(content), r.getTimestamp());
            if (pendingUser != null) {
                out.add(pendingUser);
                pendingUser = null;
            }
            out.add(assistant);
        }
        return out;
    }
}
