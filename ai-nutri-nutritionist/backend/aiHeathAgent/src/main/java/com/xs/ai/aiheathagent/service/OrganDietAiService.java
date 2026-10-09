package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.conversation.client.DeepSeekChatClient;
import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import com.xs.ai.aiheathagent.conversation.model.ConversationRole;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.prompt.Prompt;
import com.xs.ai.aiheathagent.conversation.prompt.PromptProvider;
import com.xs.ai.aiheathagent.conversation.prompt.PromptRole;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganDietAiService {
    private final DeepSeekChatClient deepSeekChatClient;
    private final PromptProvider promptProvider;

    public OrganDietAiService(DeepSeekChatClient deepSeekChatClient, PromptProvider promptProvider) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.promptProvider = promptProvider;
    }

    public String generatePlan(String userContext) {
        String ctx = String.valueOf(userContext == null ? "" : userContext).trim();
        if (ctx.isEmpty()) return "";
        if (deepSeekChatClient == null || !deepSeekChatClient.isConfigured()) return "";

        Prompt p = promptProvider.get(PromptRole.ORGAN_DIET_ANALYSIS);
        String system = p == null ? "" : String.valueOf(p.systemText() == null ? "" : p.systemText());
        List<ConversationMessage> messages = List.of(
                new ConversationMessage(ConversationRole.SYSTEM, MessageContent.text(system), LocalDateTime.now()),
                new ConversationMessage(ConversationRole.USER, MessageContent.text(ctx), LocalDateTime.now())
        );
        String out = deepSeekChatClient.complete(messages);
        return String.valueOf(out == null ? "" : out).trim();
    }
}
