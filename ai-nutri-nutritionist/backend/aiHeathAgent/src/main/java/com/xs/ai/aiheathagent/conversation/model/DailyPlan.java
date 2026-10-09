package com.xs.ai.aiheathagent.conversation.model;

import java.time.LocalDate;
import java.util.List;

public record DailyPlan(
        String planId,
        LocalDate planDate,
        MealStrategy breakfast,
        MealStrategy lunch,
        MealStrategy dinner,
        List<ReminderItem> reminders,
        ExerciseStrategy exercise,
        boolean variationFlag,
        boolean passiveOnly
) {
}
