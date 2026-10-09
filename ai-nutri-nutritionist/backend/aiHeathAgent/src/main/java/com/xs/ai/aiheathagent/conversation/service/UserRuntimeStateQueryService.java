package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.expose.SystemStateSnapshot;
import com.xs.ai.aiheathagent.conversation.model.ConversationInput;
import com.xs.ai.aiheathagent.conversation.model.InputModality;
import com.xs.ai.aiheathagent.conversation.model.UserEvent;
import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.entity.CapabilityKey;
import com.xs.ai.aiheathagent.entity.CapabilityState;
import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.repository.ReminderIntentRecordRepository;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class UserRuntimeStateQueryService {
    private final UserStateQueryService userStateQueryService;
    private final ReminderIntentRecordRepository reminderIntentRecordRepository;
    private final UserEventRecordRepository userEventRecordRepository;

    public UserRuntimeStateQueryService(
            UserStateQueryService userStateQueryService,
            ReminderIntentRecordRepository reminderIntentRecordRepository,
            UserEventRecordRepository userEventRecordRepository
    ) {
        this.userStateQueryService = userStateQueryService;
        this.reminderIntentRecordRepository = reminderIntentRecordRepository;
        this.userEventRecordRepository = userEventRecordRepository;
    }

    public SystemStateSnapshot buildForInput(Long userId, ConversationInput input, UserEvent recognizedEvent, SystemStateSnapshot.IntentView intentView) {
        LocalDateTime now = LocalDateTime.now();
        UserStateQueryService.CapabilityView cv = userStateQueryService.loadCapabilityView(userId);
        Map<CapabilityKey, CapabilityState> states = toStateMap(cv);

        var planOpt = userStateQueryService.loadDailyPlan(userId, LocalDate.now());
        SystemStateSnapshot.PlanView planView = new SystemStateSnapshot.PlanView(
                planOpt.isPresent(),
                LocalDate.now(),
                planOpt.map(p -> p.passiveOnly()).orElse(true)
        );

        boolean proactiveReminderEnabled = states.getOrDefault(CapabilityKey.PROACTIVE_REMINDER, CapabilityState.OFF) == CapabilityState.ENABLED;
        SystemStateSnapshot.ReminderGateView gateView = new SystemStateSnapshot.ReminderGateView(
                proactiveReminderEnabled,
                !proactiveReminderEnabled
        );

        long pendingCount = 0;
        ExplanationCode exp = null;
        try {
            pendingCount = reminderIntentRecordRepository.countByUserIdAndStatusAndValidUntilAfter(userId, "PENDING", now);
            if (pendingCount > 0) {
                ReminderIntentRecord first = reminderIntentRecordRepository
                        .findTopByUserIdAndStatusAndValidUntilAfterOrderByCreatedAtAscIdAsc(userId, "PENDING", now)
                        .orElse(null);
                if (first != null) {
                    exp = first.getExplanationCode();
                }
            }
        } catch (Exception ignored) {
        }
        SystemStateSnapshot.ReminderIntentView pendingIntentView = new SystemStateSnapshot.ReminderIntentView(
                pendingCount > 0,
                pendingCount,
                exp
        );

        SystemStateSnapshot.LastImageEventView lastImage = null;
        try {
            UserEventRecord rec = userEventRecordRepository.findTopByUserIdAndModalityOrderByOccurredAtDescIdDesc(userId, InputModality.IMAGE.name()).orElse(null);
            if (rec != null) {
                lastImage = new SystemStateSnapshot.LastImageEventView(rec.getId(), rec.getEventType(), rec.getOccurredAt());
            }
        } catch (Exception ignored) {
        }

        SystemStateSnapshot.EventView eventView = null;
        if (recognizedEvent != null) {
            eventView = new SystemStateSnapshot.EventView(
                    recognizedEvent.eventId(),
                    recognizedEvent.type() == null ? "" : recognizedEvent.type().name(),
                    recognizedEvent.occurredAt()
            );
        }

        InputModality modality = input == null ? InputModality.TEXT : (input.modality() == null ? InputModality.TEXT : input.modality());
        return new SystemStateSnapshot(
                userId,
                modality,
                now,
                new SystemStateSnapshot.CapabilityView(states),
                eventView,
                intentView,
                planView,
                gateView,
                pendingIntentView,
                lastImage
        );
    }

    public SystemStateSnapshot loadRuntimeState(Long userId) {
        return buildForInput(userId, null, null, null);
    }

    private Map<CapabilityKey, CapabilityState> toStateMap(UserStateQueryService.CapabilityView cv) {
        EnumMap<CapabilityKey, CapabilityState> map = new EnumMap<>(CapabilityKey.class);
        if (cv == null) {
            for (CapabilityKey k : CapabilityKey.values()) {
                map.put(k, CapabilityState.OFF);
            }
            return map;
        }
        map.put(CapabilityKey.MEDICATION_REMINDER, cv.medicationReminderEnabled());
        map.put(CapabilityKey.SUPPLEMENT_REMINDER, cv.supplementReminderEnabled());
        map.put(CapabilityKey.EXERCISE_SUGGESTION, cv.exerciseSuggestionEnabled());
        map.put(CapabilityKey.PROACTIVE_SUMMARY, cv.proactiveSummaryEnabled());
        map.put(CapabilityKey.PROACTIVE_REMINDER, cv.proactiveReminderEnabled());
        return map;
    }
}
