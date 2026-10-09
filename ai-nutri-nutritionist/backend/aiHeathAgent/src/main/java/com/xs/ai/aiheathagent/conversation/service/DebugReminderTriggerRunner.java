package com.xs.ai.aiheathagent.conversation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DebugReminderTriggerRunner implements CommandLineRunner {
    private final ReminderTriggerService reminderTriggerService;
    private final Long triggerIntentId;

    public DebugReminderTriggerRunner(
            ReminderTriggerService reminderTriggerService,
            @Value("${debug.reminder.trigger-intent-id:0}") Long triggerIntentId
    ) {
        this.reminderTriggerService = reminderTriggerService;
        this.triggerIntentId = triggerIntentId == null ? 0L : triggerIntentId;
    }

    @Override
    public void run(String... args) {
        if (triggerIntentId <= 0L) return;
        try {
            reminderTriggerService.executeById(triggerIntentId);
        } catch (Exception ignored) {
        }
    }
}

