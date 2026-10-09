package com.xs.ai.aiheathagent.conversation.model;

import java.time.LocalDateTime;

public record ConversationMessage(
        ConversationRole role,
        MessageContent content,
        LocalDateTime at
) {
}

