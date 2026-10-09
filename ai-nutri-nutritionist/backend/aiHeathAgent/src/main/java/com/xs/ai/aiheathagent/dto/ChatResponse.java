package com.xs.ai.aiheathagent.dto;

import com.xs.ai.aiheathagent.conversation.expose.SystemMessageType;
import com.xs.ai.aiheathagent.conversation.expose.SystemStateSnapshot;

import java.util.List;

public class ChatResponse {
    private String reply;
    private SystemMessageType systemMessageType;
    private SystemStateSnapshot snapshot;
    private Object meta;
    private List<String> suggestions;

    public ChatResponse() {
    }

    public ChatResponse(String reply) {
        this.reply = reply;
    }

    public ChatResponse(String reply, SystemMessageType systemMessageType, SystemStateSnapshot snapshot) {
        this.reply = reply;
        this.systemMessageType = systemMessageType;
        this.snapshot = snapshot;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public SystemMessageType getSystemMessageType() { return systemMessageType; }
    public void setSystemMessageType(SystemMessageType systemMessageType) { this.systemMessageType = systemMessageType; }

    public SystemStateSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(SystemStateSnapshot snapshot) { this.snapshot = snapshot; }

    public Object getMeta() { return meta; }
    public void setMeta(Object meta) { this.meta = meta; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
