package com.xs.ai.aiheathagent.conversation.prompt;

public interface PromptProvider {
    Prompt get(PromptRole role);
}

