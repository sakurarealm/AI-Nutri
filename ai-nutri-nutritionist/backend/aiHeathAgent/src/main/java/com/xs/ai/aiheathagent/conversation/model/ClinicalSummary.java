package com.xs.ai.aiheathagent.conversation.model;

import java.util.List;

public record ClinicalSummary(
        String schemaVersion,
        List<ClinicalDatum> data
) {
}

