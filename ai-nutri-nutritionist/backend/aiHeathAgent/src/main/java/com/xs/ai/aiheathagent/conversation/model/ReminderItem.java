package com.xs.ai.aiheathagent.conversation.model;

public record ReminderItem(
        String itemKey,
        String category,
        String title,
        String timeHint
) {
}

