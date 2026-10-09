package com.xs.ai.aiheathagent.conversation.model;

public record PlanAction(
        String domain,
        String action,
        String instruction
) {
}

