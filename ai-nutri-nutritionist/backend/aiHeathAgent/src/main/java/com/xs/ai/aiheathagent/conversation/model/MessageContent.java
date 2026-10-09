package com.xs.ai.aiheathagent.conversation.model;

public record MessageContent(
        MessageContentType type,
        String text
) {
    public static MessageContent text(String text) {
        return new MessageContent(MessageContentType.TEXT, String.valueOf(text == null ? "" : text));
    }
}

