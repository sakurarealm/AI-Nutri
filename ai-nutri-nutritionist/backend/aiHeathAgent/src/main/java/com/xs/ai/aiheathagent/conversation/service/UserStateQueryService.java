package com.xs.ai.aiheathagent.conversation.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xs.ai.aiheathagent.conversation.model.DailyPlan;
import com.xs.ai.aiheathagent.entity.CapabilityState;
import com.xs.ai.aiheathagent.entity.DailyPlanRecord;
import com.xs.ai.aiheathagent.entity.DailySummaryRecord;
import com.xs.ai.aiheathagent.entity.UserCapabilityProfile;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.DailyPlanRecordRepository;
import com.xs.ai.aiheathagent.repository.DailySummaryRecordRepository;
import com.xs.ai.aiheathagent.repository.UserCapabilityProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class UserStateQueryService {
    private final UserCapabilityProfileRepository capabilityProfileRepository;
    private final DailyPlanRecordRepository dailyPlanRecordRepository;
    private final DailySummaryRecordRepository dailySummaryRecordRepository;
    private final ConversationRecordRepository conversationRecordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .registerModule(new JavaTimeModule());

    public UserStateQueryService(
            UserCapabilityProfileRepository capabilityProfileRepository,
            DailyPlanRecordRepository dailyPlanRecordRepository,
            DailySummaryRecordRepository dailySummaryRecordRepository,
            ConversationRecordRepository conversationRecordRepository
    ) {
        this.capabilityProfileRepository = capabilityProfileRepository;
        this.dailyPlanRecordRepository = dailyPlanRecordRepository;
        this.dailySummaryRecordRepository = dailySummaryRecordRepository;
        this.conversationRecordRepository = conversationRecordRepository;
    }

    public CapabilityView loadCapabilityView(Long userId) {
        UserCapabilityProfile p = userId == null ? null : capabilityProfileRepository.findByUserId(userId).orElse(null);
        if (p == null) {
            return CapabilityView.allOff(userId);
        }
        return new CapabilityView(
                p.getUserId(),
                safe(p.getMedicationReminderEnabled()),
                safe(p.getSupplementReminderEnabled()),
                safe(p.getExerciseSuggestionEnabled()),
                safe(p.getProactiveSummaryEnabled()),
                safe(p.getProactiveReminderEnabled())
        );
    }

    public Optional<DailyPlan> loadLatestDailyPlan(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return dailyPlanRecordRepository.findTopByUserIdOrderByPlanDateDescIdDesc(userId).flatMap(this::toPlan);
    }

    public Optional<DailyPlan> loadDailyPlan(Long userId, LocalDate date) {
        if (userId == null || date == null) {
            return Optional.empty();
        }
        return dailyPlanRecordRepository.findTopByUserIdAndPlanDateOrderByIdDesc(userId, date).flatMap(this::toPlan);
    }

    public Optional<DailySummaryRecord> loadLatestDailySummary(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return dailySummaryRecordRepository.findTopByUserIdOrderByDateDescIdDesc(userId);
    }

    public List<com.xs.ai.aiheathagent.entity.ConversationRecord> loadInbox(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        int safe = Math.max(1, Math.min(limit, 200));
        String sessionId = "inbox_" + userId;
        return conversationRecordRepository.findBySessionIdAndUserIdOrderByIdDesc(sessionId, userId, PageRequest.of(0, safe)).getContent();
    }

    private Optional<DailyPlan> toPlan(DailyPlanRecord rec) {
        if (rec == null || rec.getPlanJson() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(rec.getPlanJson(), DailyPlan.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private CapabilityState safe(CapabilityState s) {
        return s == null ? CapabilityState.OFF : s;
    }

    public record CapabilityView(
            Long userId,
            CapabilityState medicationReminderEnabled,
            CapabilityState supplementReminderEnabled,
            CapabilityState exerciseSuggestionEnabled,
            CapabilityState proactiveSummaryEnabled,
            CapabilityState proactiveReminderEnabled
    ) {
        public static CapabilityView allOff(Long userId) {
            return new CapabilityView(
                    userId,
                    CapabilityState.OFF,
                    CapabilityState.OFF,
                    CapabilityState.OFF,
                    CapabilityState.ENABLED,
                    CapabilityState.OFF
            );
        }
    }
}
