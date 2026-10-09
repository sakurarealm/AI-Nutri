package com.xs.ai.aiheathagent.conversation.model;

import java.time.LocalDateTime;
import java.util.Set;

public record UserEvent(
        Long eventId,
        Long userId,
        String sessionId,
        LocalDateTime occurredAt,
        UserEventType type,
        Set<UserEventTopic> topics,
        double confidence,
        boolean suggestOpenCamera,
        boolean suggestWriteNutrition,
        MessageContent originalContent,
        InputModality modality
) {
}

