package com.xs.ai.aiheathagent.conversation.model;

import java.time.LocalDate;
import java.util.List;

public record PlanResult(
        Long userId,
        LocalDate planDate,
        String planId,
        List<PlanAction> actions
) {
}

