package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.DecisionActionType;
import com.xs.ai.aiheathagent.conversation.model.DecisionResult;
import com.xs.ai.aiheathagent.conversation.model.UserEvent;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.entity.CapabilityState;
import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.entity.UserCapabilityProfile;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.UserCapabilityProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
public class HealthDecisionService {
    private static final String ONBOARDING_PREFIX = "新手引导｜";

    private final ConversationRecordRepository conversationRepository;
    private final UserCapabilityProfileRepository userCapabilityProfileRepository;

    private final boolean proactiveInboxEnabled;
    private final long proactiveInboxMinGapMinutes;
    private final long proactiveInboxActiveGapMinutes;
    private final long proactiveInboxActiveWindowMinutes;
    private final int proactiveInboxMaxPerDay;
    private final int proactiveInboxQuietStartHour;
    private final int proactiveInboxQuietEndHour;
    private final int proactiveOnlyLoginWithinDays;

    public HealthDecisionService(
            ConversationRecordRepository conversationRepository,
            UserCapabilityProfileRepository userCapabilityProfileRepository,
            @Value("${inbox.proactive.enabled:true}") boolean proactiveInboxEnabled,
            @Value("${inbox.proactive.min-gap-minutes:120}") long proactiveInboxMinGapMinutes,
            @Value("${inbox.proactive.active-gap-minutes:3}") long proactiveInboxActiveGapMinutes,
            @Value("${inbox.proactive.active-window-minutes:240}") long proactiveInboxActiveWindowMinutes,
            @Value("${inbox.proactive.max-per-day:4}") int proactiveInboxMaxPerDay,
            @Value("${inbox.proactive.quiet-start-hour:23}") int proactiveInboxQuietStartHour,
            @Value("${inbox.proactive.quiet-end-hour:7}") int proactiveInboxQuietEndHour,
            @Value("${inbox.proactive.only-login-within-days:30}") int proactiveOnlyLoginWithinDays
    ) {
        this.conversationRepository = conversationRepository;
        this.userCapabilityProfileRepository = userCapabilityProfileRepository;
        this.proactiveInboxEnabled = proactiveInboxEnabled;
        this.proactiveInboxMinGapMinutes = proactiveInboxMinGapMinutes;
        this.proactiveInboxActiveGapMinutes = proactiveInboxActiveGapMinutes;
        this.proactiveInboxActiveWindowMinutes = proactiveInboxActiveWindowMinutes;
        this.proactiveInboxMaxPerDay = proactiveInboxMaxPerDay;
        this.proactiveInboxQuietStartHour = proactiveInboxQuietStartHour;
        this.proactiveInboxQuietEndHour = proactiveInboxQuietEndHour;
        this.proactiveOnlyLoginWithinDays = proactiveOnlyLoginWithinDays;
    }

    public DecisionResult decideRealtime(User user, UserEvent event) {
        Long userId = user == null ? null : user.getId();
        String key = "realtime:" + String.valueOf(userId);
        String windowId = windowId(LocalDateTime.now());
        return DecisionResult.chatOnly(userId, key, windowId, LocalDateTime.now().plusMinutes(2));
    }

    public DecisionResult decideOnInboxPull(User user, LocalDateTime now) {
        return decideProactiveInbox(user, now, "pull");
    }

    public DecisionResult decideOnScheduledTick(User user, LocalDateTime now) {
        return decideProactiveInbox(user, now, "tick");
    }

    public DecisionResult decideForReminderIntent(User user, ReminderIntentRecord intent, LocalDateTime now) {
        Long userId = user == null ? null : user.getId();
        if (intent == null || userId == null) {
            return new DecisionResult(userId, "noop", windowId(now), now.plusMinutes(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        UserCapabilityProfile cap = loadCapability(userId);
        if (!isEnabled(cap == null ? null : cap.getProactiveReminderEnabled())) {
            return new DecisionResult(userId, intent.getDecisionKey(), String.valueOf(intent.getWindowId()), intent.getValidUntil(), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (!isItemAllowed(cap, intent.getItemKey())) {
            return new DecisionResult(userId, intent.getDecisionKey(), String.valueOf(intent.getWindowId()), intent.getValidUntil(), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (!proactiveInboxEnabled) {
            return new DecisionResult(userId, intent.getDecisionKey(), String.valueOf(intent.getWindowId()), intent.getValidUntil(), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return new DecisionResult(userId, intent.getDecisionKey(), String.valueOf(intent.getWindowId()), intent.getValidUntil(), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (inQuietHours(now)) {
            return new DecisionResult(userId, intent.getDecisionKey(), String.valueOf(intent.getWindowId()), intent.getValidUntil(), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (!shouldProactiveForUser(user, now)) {
            return new DecisionResult(userId, intent.getDecisionKey(), String.valueOf(intent.getWindowId()), intent.getValidUntil(), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (intent.getValidUntil() != null && now != null && now.isAfter(intent.getValidUntil())) {
            return new DecisionResult(userId, intent.getDecisionKey(), String.valueOf(intent.getWindowId()), intent.getValidUntil(), EnumSet.of(DecisionActionType.NONE), false);
        }
        return new DecisionResult(userId, intent.getDecisionKey(), String.valueOf(intent.getWindowId()), intent.getValidUntil(), EnumSet.of(DecisionActionType.PROACTIVE_INBOX), false);
    }

    public DecisionResult decideDailyPlanGeneration(User user, LocalDate planDate, LocalDateTime now) {
        Long userId = user == null ? null : user.getId();
        if (userId == null || planDate == null) {
            return new DecisionResult(userId, "noop", windowId(now), now.plusMinutes(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        loadCapability(userId);
        if (user.getStatus() != null && user.getStatus() == 0) {
            return new DecisionResult(userId, "noop", windowId(now), now.plusMinutes(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (!shouldProactiveForUser(user, now == null ? LocalDateTime.now() : now)) {
            return new DecisionResult(userId, "plan:inactive", planDate.toString(), (now == null ? LocalDateTime.now() : now).plusHours(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        String win = "dailyplan:" + planDate;
        String key = "dailyplan:" + userId + ":" + planDate;
        LocalDateTime validUntil = (now == null ? LocalDateTime.now() : now).plusHours(2);
        return new DecisionResult(userId, key, win, validUntil, EnumSet.of(DecisionActionType.GENERATE_DAILY_PLAN), false);
    }

    public DecisionResult decideDailySummaryGeneration(User user, LocalDate date, LocalDateTime now) {
        Long userId = user == null ? null : user.getId();
        if (userId == null || date == null) {
            return new DecisionResult(userId, "noop", windowId(now), now.plusMinutes(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        UserCapabilityProfile cap = loadCapability(userId);
        if (cap != null && !isEnabled(cap.getProactiveSummaryEnabled())) {
            return new DecisionResult(userId, "summary:disabled", date.toString(), (now == null ? LocalDateTime.now() : now).plusHours(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return new DecisionResult(userId, "noop", windowId(now), now.plusMinutes(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (!shouldProactiveForUser(user, now == null ? LocalDateTime.now() : now)) {
            return new DecisionResult(userId, "summary:inactive", date.toString(), (now == null ? LocalDateTime.now() : now).plusHours(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        String win = "summary:" + date;
        String key = "summary:" + userId + ":" + date;
        LocalDateTime validUntil = (now == null ? LocalDateTime.now() : now).plusHours(4);
        return new DecisionResult(userId, key, win, validUntil, EnumSet.of(DecisionActionType.GENERATE_SUMMARY), false);
    }

    private DecisionResult decideProactiveInbox(User user, LocalDateTime now, String source) {
        Long userId = user == null ? null : user.getId();
        if (!proactiveInboxEnabled || userId == null) {
            return new DecisionResult(userId, "noop", windowId(now), now.plusMinutes(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        UserCapabilityProfile cap = loadCapability(userId);
        if (!isEnabled(cap == null ? null : cap.getProactiveReminderEnabled())) {
            return new DecisionResult(userId, "disabled", windowId(now), now.plusMinutes(30), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return new DecisionResult(userId, "noop", windowId(now), now.plusMinutes(1), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (inQuietHours(now)) {
            return new DecisionResult(userId, "quiet", windowId(now), now.plusMinutes(30), EnumSet.of(DecisionActionType.NONE), false);
        }
        if (!shouldProactiveForUser(user, now)) {
            return new DecisionResult(userId, "inactive", windowId(now), now.plusMinutes(30), EnumSet.of(DecisionActionType.NONE), false);
        }

        String sessionId = inboxSessionId(userId);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        long pushedToday = conversationRepository.countBySessionIdAndUserIdAndTimestampAfterExcludePrefix(sessionId, userId, todayStart, ONBOARDING_PREFIX);
        if (pushedToday >= Math.max(1, proactiveInboxMaxPerDay)) {
            return new DecisionResult(userId, "max_per_day:" + pushedToday, windowId(now), todayStart.plusDays(1), EnumSet.of(DecisionActionType.NONE), false);
        }

        Optional<ConversationRecord> lastUserChat = conversationRepository
                .findTopByUserIdAndMessageTypeAndSessionIdNotOrderByTimestampDescIdDesc(userId, ConversationRecord.MessageType.USER, sessionId);

        Optional<ConversationRecord> lastPush = findLastNonOnboardingInboxMessage(sessionId, userId);
        LocalDateTime lastPushAt = lastPush.isPresent() ? lastPush.get().getTimestamp() : null;

        boolean engaged = false;
        if (lastPushAt != null && lastUserChat.isPresent() && lastUserChat.get().getTimestamp() != null) {
            engaged = lastUserChat.get().getTimestamp().isAfter(lastPushAt);
        }

        long gapMinutes = resolveGapMinutes(now, lastUserChat.orElse(null), engaged);
        LocalDateTime cutoff = now.minusMinutes(Math.max(1, gapMinutes));
        if (lastPushAt != null && lastPushAt.isAfter(cutoff)) {
            return new DecisionResult(userId, "gap", windowId(now), cutoff.plusMinutes(gapMinutes), EnumSet.of(DecisionActionType.NONE), false);
        }

        String win = windowId(now);
        String decisionKey = "inbox:" + userId + ":" + win + ":" + source;
        LocalDateTime validUntil = windowStart(now).plusMinutes(15);
        return new DecisionResult(userId, decisionKey, win, validUntil, EnumSet.of(DecisionActionType.PROACTIVE_INBOX), false);
    }

    private Optional<ConversationRecord> findLastNonOnboardingInboxMessage(String sessionId, Long userId) {
        try {
            List<ConversationRecord> list = conversationRepository.findLatestBySessionIdAndUserIdExcludePrefix(
                    sessionId,
                    userId,
                    ONBOARDING_PREFIX,
                    PageRequest.of(0, 1)
            );
            if (list != null && !list.isEmpty()) {
                return Optional.ofNullable(list.get(0));
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private boolean inQuietHours(LocalDateTime now) {
        int start = normalizeHour(proactiveInboxQuietStartHour);
        int end = normalizeHour(proactiveInboxQuietEndHour);
        int hour = now.getHour();
        if (start == end) {
            return false;
        }
        if (start < end) {
            return hour >= start && hour < end;
        }
        return hour >= start || hour < end;
    }

    private int normalizeHour(int hour) {
        int h = hour % 24;
        if (h < 0) {
            h += 24;
        }
        return h;
    }

    private boolean shouldProactiveForUser(User user, LocalDateTime now) {
        int days = Math.max(1, proactiveOnlyLoginWithinDays);
        LocalDateTime cutoff = now.minusDays(days);
        LocalDateTime t = user.getLastLoginAt();
        if (t == null) {
            t = user.getUpdatedAt();
        }
        if (t == null) {
            t = user.getCreatedAt();
        }
        if (t == null) {
            return false;
        }
        return !t.isBefore(cutoff);
    }

    private long resolveGapMinutes(LocalDateTime now, ConversationRecord lastUserChat, boolean engaged) {
        if (engaged) {
            return Math.max(1, proactiveInboxActiveGapMinutes);
        }
        long base = Math.max(1, proactiveInboxMinGapMinutes);
        if (lastUserChat == null || lastUserChat.getTimestamp() == null) {
            return base;
        }
        LocalDateTime activeCutoff = now.minusMinutes(Math.max(1, proactiveInboxActiveWindowMinutes));
        if (lastUserChat.getTimestamp().isAfter(activeCutoff)) {
            return Math.max(1, proactiveInboxActiveGapMinutes);
        }
        return base;
    }

    private String inboxSessionId(Long userId) {
        return "inbox_" + String.valueOf(userId);
    }

    private String windowId(LocalDateTime now) {
        LocalDateTime ws = windowStart(now);
        return ws.toLocalDate() + "_" + ws.getHour() + "_" + (ws.getMinute() / 15);
    }

    private LocalDateTime windowStart(LocalDateTime now) {
        LocalDateTime n = now == null ? LocalDateTime.now() : now;
        int minuteBucket = (n.getMinute() / 15) * 15;
        return n.truncatedTo(ChronoUnit.HOURS).plusMinutes(minuteBucket);
    }

    private UserCapabilityProfile loadCapability(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return userCapabilityProfileRepository.findByUserId(userId).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isEnabled(CapabilityState state) {
        return state == CapabilityState.ENABLED;
    }

    private boolean isItemAllowed(UserCapabilityProfile cap, String itemKey) {
        String key = String.valueOf(itemKey == null ? "" : itemKey).trim();
        if (key.isEmpty()) {
            return false;
        }
        if (key.startsWith("meal_")) {
            return true;
        }
        if (key.startsWith("medication_")) {
            return isEnabled(cap == null ? null : cap.getMedicationReminderEnabled());
        }
        if (key.startsWith("supplement_")) {
            return isEnabled(cap == null ? null : cap.getSupplementReminderEnabled());
        }
        if (key.startsWith("exercise_")) {
            return isEnabled(cap == null ? null : cap.getExerciseSuggestionEnabled());
        }
        if (key.startsWith("summary_")) {
            return isEnabled(cap == null ? null : cap.getProactiveSummaryEnabled());
        }
        return false;
    }
}
