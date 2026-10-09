package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.DecisionActionType;
import com.xs.ai.aiheathagent.conversation.model.DecisionResult;
import com.xs.ai.aiheathagent.conversation.model.DailyPlan;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repository.ReminderIntentRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ReminderTriggerService {
    private final UserRepo userRepo;
    private final HealthDecisionService healthDecisionService;
    private final ProactiveTouchService proactiveTouchService;
    private final ReminderIntentRecordRepository reminderIntentRecordRepository;
    private final DailyPlanService dailyPlanService;
    private final ReminderComposeService reminderComposeService;

    private final LocalTime morningStart;
    private final LocalTime noonStart;
    private final LocalTime afternoonStart;
    private final LocalTime eveningStart;
    private final LocalTime nightStart;
    private final int quietStartHour;
    private final int quietEndHour;

    public ReminderTriggerService(
            UserRepo userRepo,
            HealthDecisionService healthDecisionService,
            ProactiveTouchService proactiveTouchService,
            ReminderIntentRecordRepository reminderIntentRecordRepository,
            DailyPlanService dailyPlanService,
            ReminderComposeService reminderComposeService,
            @Value("${timeslot.morning.start:07:00}") String morningStart,
            @Value("${timeslot.noon.start:12:00}") String noonStart,
            @Value("${timeslot.afternoon.start:15:00}") String afternoonStart,
            @Value("${timeslot.evening.start:18:00}") String eveningStart,
            @Value("${timeslot.night.start:21:00}") String nightStart,
            @Value("${reminder.quiet-start-hour:23}") int quietStartHour,
            @Value("${reminder.quiet-end-hour:7}") int quietEndHour
    ) {
        this.userRepo = userRepo;
        this.healthDecisionService = healthDecisionService;
        this.proactiveTouchService = proactiveTouchService;
        this.reminderIntentRecordRepository = reminderIntentRecordRepository;
        this.dailyPlanService = dailyPlanService;
        this.reminderComposeService = reminderComposeService;
        this.morningStart = parseLocalTime(morningStart, LocalTime.of(7, 0));
        this.noonStart = parseLocalTime(noonStart, LocalTime.of(12, 0));
        this.afternoonStart = parseLocalTime(afternoonStart, LocalTime.of(15, 0));
        this.eveningStart = parseLocalTime(eveningStart, LocalTime.of(18, 0));
        this.nightStart = parseLocalTime(nightStart, LocalTime.of(21, 0));
        this.quietStartHour = quietStartHour;
        this.quietEndHour = quietEndHour;
    }

    @Transactional
    public boolean executeOne(ReminderIntentRecord intent, LocalDateTime now) {
        return executeOne(intent, null, now);
    }

    @Transactional
    public boolean executeOne(ReminderIntentRecord intent, User preloadedUser, LocalDateTime now) {
        if (intent == null || intent.getUserId() == null) {
            return false;
        }
        LocalDateTime at = now == null ? LocalDateTime.now() : now;
        if (intent.getValidUntil() != null && at.isAfter(intent.getValidUntil())) {
            mark(intent, "EXPIRED", at);
            return false;
        }
        User user = preloadedUser;
        if (user == null || user.getId() == null || !intent.getUserId().equals(user.getId())) {
            user = userRepo.findById(intent.getUserId()).orElse(null);
        }
        if (user == null || user.getId() == null) {
            mark(intent, "SKIPPED", at);
            return false;
        }
        if (!isDueNow(intent, at)) {
            return false;
        }
        DecisionResult decision = healthDecisionService.decideForReminderIntent(user, intent, at);
        if (decision.actions() == null || !decision.actions().contains(DecisionActionType.PROACTIVE_INBOX)) {
            return false;
        }
        DailyPlan plan = dailyPlanService.findPlan(user.getId(), intent.getPlanDate()).orElse(null);
        MessageContent content = reminderComposeService.compose(user, plan, intent);
        boolean ok = proactiveTouchService.executeInboxTouch(user, decision, content, intent.getExplanationCode(), intent.getExplanationArgs());
        mark(intent, ok ? "EXECUTED" : "SKIPPED", at);
        return ok;
    }

    @Transactional
    public boolean executeById(Long intentId) {
        if (intentId == null) return false;
        ReminderIntentRecord intent = reminderIntentRecordRepository.findById(intentId).orElse(null);
        if (intent == null) return false;
        return executeOne(intent, LocalDateTime.now());
    }

    private void mark(ReminderIntentRecord intent, String status, LocalDateTime now) {
        try {
            intent.setStatus(status);
            intent.setExecutedAt(now);
            reminderIntentRecordRepository.save(intent);
        } catch (Exception ignored) {
        }
    }

    private boolean isDueNow(ReminderIntentRecord intent, LocalDateTime now) {
        if (intent == null || now == null) return false;
        String slot = String.valueOf(intent.getTimeSlot() == null ? "" : intent.getTimeSlot()).trim();
        if (slot.isEmpty()) return true;
        if (intent.getPlanDate() == null) return true;
        try {
            LocalTime lt = parseTimeSlot(slot);
            if (lt == null) return true;
            LocalDateTime due = intent.getPlanDate().atTime(lt);
            if (now.isBefore(due)) return false;
            LocalDateTime end = due.plusMinutes(90);
            if (isPlanWindow(intent) && intent.getValidUntil() != null) {
                end = intent.getValidUntil();
            }
            return !now.isAfter(end);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isPlanWindow(ReminderIntentRecord intent) {
        String w = String.valueOf(intent == null ? "" : (intent.getWindowId() == null ? "" : intent.getWindowId())).trim();
        if (w.isEmpty()) return false;
        int idx = w.lastIndexOf(':');
        if (idx < 0 || idx >= w.length() - 1) return false;
        String tail = w.substring(idx + 1).trim().toUpperCase();
        return tail.equals("MORNING") || tail.equals("NOON") || tail.equals("AFTERNOON") || tail.equals("EVENING") || tail.equals("NIGHT");
    }

    private LocalTime parseTimeSlot(String timeSlot) {
        String t = String.valueOf(timeSlot == null ? "" : timeSlot).trim();
        if (t.isEmpty()) return null;
        if (t.matches("\\d{2}:\\d{2}")) {
            try {
                return clampQuietHours(LocalTime.parse(t));
            } catch (Exception ignored) {
                return null;
            }
        }
        String up = t.toUpperCase();
        LocalTime base = switch (up) {
            case "MORNING" -> morningStart;
            case "NOON" -> noonStart;
            case "AFTERNOON" -> afternoonStart;
            case "EVENING" -> eveningStart;
            case "NIGHT" -> nightStart;
            default -> null;
        };
        return clampQuietHours(base);
    }

    private LocalTime clampQuietHours(LocalTime t) {
        if (t == null) return null;
        int start = normalizeHour(quietStartHour);
        int end = normalizeHour(quietEndHour);
        if (start == end) return t;
        LocalTime startTime = LocalTime.of(start, 0);
        LocalTime endTime = LocalTime.of(end, 0);
        boolean inQuiet;
        if (start < end) {
            inQuiet = !t.isBefore(startTime) && t.isBefore(endTime);
            if (!inQuiet) return t;
            return endTime;
        }
        inQuiet = !t.isBefore(startTime) || t.isBefore(endTime);
        if (!inQuiet) return t;
        if (!t.isBefore(startTime)) {
            if (startTime.equals(LocalTime.MIDNIGHT)) return LocalTime.of(23, 59);
            return startTime.minusMinutes(1);
        }
        return endTime;
    }

    private static int normalizeHour(int hour) {
        int h = hour % 24;
        if (h < 0) h += 24;
        return h;
    }

    private static LocalTime parseLocalTime(String raw, LocalTime fallback) {
        try {
            String t = String.valueOf(raw == null ? "" : raw).trim();
            if (t.isEmpty()) return fallback;
            return LocalTime.parse(t);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
