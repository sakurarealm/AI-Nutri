package com.xs.ai.aiheathagent.conversation.model;

import java.time.LocalDateTime;
import java.util.EnumSet;

public record DecisionResult(
        Long userId,
        String decisionKey,
        String windowId,
        LocalDateTime validUntil,
        EnumSet<DecisionActionType> actions,
        boolean allowChat
) {
    public static DecisionResult chatOnly(Long userId, String decisionKey, String windowId, LocalDateTime validUntil) {
        return new DecisionResult(
                userId,
                decisionKey,
                windowId,
                validUntil,
                EnumSet.of(DecisionActionType.NONE),
                true
        );
    }
}

