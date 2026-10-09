package com.xs.ai.aiheathagent.conversation.model;

import java.time.LocalDate;

public record DailySummary(
        Long userId,
        LocalDate date,
        MessageContent userFacing,
        MessageContent familyFacing,
        ClinicalSummary clinicalSummary
) {
}
