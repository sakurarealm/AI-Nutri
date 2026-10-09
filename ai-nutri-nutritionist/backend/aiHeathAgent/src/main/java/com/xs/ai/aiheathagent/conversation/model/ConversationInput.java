package com.xs.ai.aiheathagent.conversation.model;

import com.xs.ai.aiheathagent.vision.FoodCandidate;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationInput(
        String sessionId,
        Long userId,
        InputModality modality,
        MessageContent content,
        LocalDateTime receivedAt,
        List<FoodCandidate> foodCandidates
) {
    public static ConversationInput text(String sessionId, Long userId, String text) {
        return new ConversationInput(
                String.valueOf(sessionId == null ? "" : sessionId).trim(),
                userId,
                InputModality.TEXT,
                MessageContent.text(text),
                LocalDateTime.now(),
                null
        );
    }

    public static ConversationInput image(String sessionId, Long userId, List<FoodCandidate> candidates) {
        return new ConversationInput(
                String.valueOf(sessionId == null ? "" : sessionId).trim(),
                userId,
                InputModality.IMAGE,
                MessageContent.text(""),
                LocalDateTime.now(),
                candidates
        );
    }
}
