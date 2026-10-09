package com.xs.ai.aiheathagent.conversation.expose;

import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.conversation.model.InputModality;
import com.xs.ai.aiheathagent.entity.CapabilityKey;
import com.xs.ai.aiheathagent.entity.CapabilityState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record SystemStateSnapshot(
        Long userId,
        InputModality inputModality,
        LocalDateTime snapshotAt,
        CapabilityView capability,
        EventView recognizedEvent,
        IntentView recognizedIntent,
        PlanView dailyPlan,
        ReminderGateView reminderGate,
        ReminderIntentView pendingReminderIntent,
        LastImageEventView lastImageEvent
) {
    public record CapabilityView(
            Map<CapabilityKey, CapabilityState> states
    ) {
    }

    public record EventView(
            Long eventId,
            String eventType,
            LocalDateTime occurredAt
    ) {
    }

    public record IntentView(
            String intentType,
            String scheduleTime,
            String subject,
            CapabilityKey requiredCapability,
            CapabilityState requiredCapabilityState,
            boolean blocked
    ) {
    }

    public record PlanView(
            boolean exists,
            LocalDate planDate,
            boolean passiveOnly
    ) {
    }

    public record ReminderGateView(
            boolean proactiveReminderEnabled,
            boolean blockedByCapability
    ) {
    }

    public record ReminderIntentView(
            boolean exists,
            long pendingCount,
            ExplanationCode explanationCode
    ) {
    }

    public record LastImageEventView(
            Long eventId,
            String eventType,
            LocalDateTime occurredAt
    ) {
    }
}
