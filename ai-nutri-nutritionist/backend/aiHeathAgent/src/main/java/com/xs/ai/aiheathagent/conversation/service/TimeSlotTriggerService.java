package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.conversation.model.DailyPlan;
import com.xs.ai.aiheathagent.conversation.model.ReminderItem;
import com.xs.ai.aiheathagent.conversation.model.TimeSlot;
import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repository.ReminderIntentRecordRepository;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TimeSlotTriggerService {
    private final UserRepo userRepo;
    private final DailyPlanService dailyPlanService;
    private final UserEventRecordRepository userEventRecordRepository;
    private final ReminderIntentRecordRepository reminderIntentRecordRepository;

    private final LocalTime morningStart;
    private final LocalTime morningEnd;
    private final LocalTime noonStart;
    private final LocalTime noonEnd;
    private final LocalTime afternoonStart;
    private final LocalTime afternoonEnd;
    private final LocalTime eveningStart;
    private final LocalTime eveningEnd;
    private final LocalTime nightStart;
    private final LocalTime nightEnd;
    private final int startWindowMinutes;
    private final int onlyLoginWithinDays;
    private final int scanPageSize;

    public TimeSlotTriggerService(
            UserRepo userRepo,
            DailyPlanService dailyPlanService,
            UserEventRecordRepository userEventRecordRepository,
            ReminderIntentRecordRepository reminderIntentRecordRepository,
            @Value("${timeslot.morning.start:07:00}") String morningStart,
            @Value("${timeslot.morning.end:09:30}") String morningEnd,
            @Value("${timeslot.noon.start:12:00}") String noonStart,
            @Value("${timeslot.noon.end:13:30}") String noonEnd,
            @Value("${timeslot.afternoon.start:15:00}") String afternoonStart,
            @Value("${timeslot.afternoon.end:17:00}") String afternoonEnd,
            @Value("${timeslot.evening.start:18:00}") String eveningStart,
            @Value("${timeslot.evening.end:20:00}") String eveningEnd,
            @Value("${timeslot.night.start:21:00}") String nightStart,
            @Value("${timeslot.night.end:22:30}") String nightEnd,
            @Value("${timeslot.trigger.start-window-minutes:90}") int startWindowMinutes,
            @Value("${timeslot.trigger.only-login-within-days:30}") int onlyLoginWithinDays,
            @Value("${timeslot.trigger.scan-page-size:500}") int scanPageSize
    ) {
        this.userRepo = userRepo;
        this.dailyPlanService = dailyPlanService;
        this.userEventRecordRepository = userEventRecordRepository;
        this.reminderIntentRecordRepository = reminderIntentRecordRepository;
        this.morningStart = LocalTime.parse(morningStart);
        this.morningEnd = LocalTime.parse(morningEnd);
        this.noonStart = LocalTime.parse(noonStart);
        this.noonEnd = LocalTime.parse(noonEnd);
        this.afternoonStart = LocalTime.parse(afternoonStart);
        this.afternoonEnd = LocalTime.parse(afternoonEnd);
        this.eveningStart = LocalTime.parse(eveningStart);
        this.eveningEnd = LocalTime.parse(eveningEnd);
        this.nightStart = LocalTime.parse(nightStart);
        this.nightEnd = LocalTime.parse(nightEnd);
        this.startWindowMinutes = Math.max(1, startWindowMinutes);
        this.onlyLoginWithinDays = onlyLoginWithinDays;
        this.scanPageSize = Math.max(20, scanPageSize);
    }

    @Scheduled(fixedDelayString = "${timeslot.trigger.interval-ms:600000}")
    public void tick() {
        tickAt(LocalDateTime.now());
    }

    public void tickAt(LocalDateTime now) {
        TimeSlot slot = resolveSlot(now.toLocalTime());
        if (slot == null) {
            return;
        }
        LocalDate date = now.toLocalDate();
        LocalDateTime after = now.minusDays(Math.max(0, onlyLoginWithinDays));
        int page = 0;
        while (true) {
            Page<User> users;
            try {
                users = onlyLoginWithinDays <= 0
                        ? userRepo.findActiveUsers(PageRequest.of(page, scanPageSize))
                        : userRepo.findActiveUsersLoggedInAfter(after, PageRequest.of(page, scanPageSize));
            } catch (Exception e) {
                users = Page.empty();
            }
            if (users == null || users.isEmpty()) break;
            for (User u : users.getContent()) {
                try {
                    if (u == null || u.getId() == null) {
                        continue;
                    }
                    if (u.getStatus() != null && u.getStatus() == 0) {
                        continue;
                    }
                    produceIntentsForUser(u, date, slot, now);
                } catch (Exception ignored) {
                }
            }
            if (!users.hasNext()) break;
            page++;
        }
    }

    public void tickAtStrict(LocalDateTime now) {
        TimeSlot slot = resolveSlot(now.toLocalTime());
        if (slot == null) {
            return;
        }
        LocalDate date = now.toLocalDate();
        LocalDateTime after = now.minusDays(Math.max(0, onlyLoginWithinDays));
        int page = 0;
        while (true) {
            Page<User> users = onlyLoginWithinDays <= 0
                    ? userRepo.findActiveUsers(PageRequest.of(page, scanPageSize))
                    : userRepo.findActiveUsersLoggedInAfter(after, PageRequest.of(page, scanPageSize));
            if (users == null || users.isEmpty()) break;
            for (User u : users.getContent()) {
                if (u == null || u.getId() == null) {
                    continue;
                }
                if (u.getStatus() != null && u.getStatus() == 0) {
                    continue;
                }
                produceIntentsForUser(u, date, slot, now);
            }
            if (!users.hasNext()) break;
            page++;
        }
    }

    private void produceIntentsForUser(User user, LocalDate date, TimeSlot slot, LocalDateTime now) {
        Optional<DailyPlan> op = dailyPlanService.findPlan(user.getId(), date);
        if (op.isEmpty()) {
            return;
        }
        DailyPlan plan = op.get();
        if (plan.passiveOnly()) {
            return;
        }
        LocalDateTime slotStart = date.atTime(slotStart(slot));
        LocalDateTime slotEnd = date.atTime(slotEnd(slot));
        if (now.isBefore(slotStart) || now.isAfter(slotEnd)) {
            return;
        }
        if (now.isAfter(slotStart.plusMinutes(startWindowMinutes))) {
            return;
        }
        long mealCount = 0;
        long medicineCount = 0;
        try {
            mealCount = userEventRecordRepository.countByUserIdAndEventTypeAndOccurredAtBetween(user.getId(), "MEAL", slotStart, slotEnd);
        } catch (Exception ignored) {
        }
        try {
            medicineCount = userEventRecordRepository.countByUserIdAndEventTypeAndOccurredAtBetween(user.getId(), "MEDICINE", slotStart, slotEnd);
        } catch (Exception ignored) {
        }
        boolean hasMeal = mealCount > 0;
        boolean hasMedicine = medicineCount > 0;

        Set<String> existingDecisionKeys = new HashSet<>();
        try {
            List<ReminderIntentRecord> existing = reminderIntentRecordRepository.findByUserIdAndPlanDate(user.getId(), date);
            for (ReminderIntentRecord r : existing) {
                if (r == null) continue;
                String dk = String.valueOf(r.getDecisionKey() == null ? "" : r.getDecisionKey()).trim();
                if (!dk.isEmpty()) existingDecisionKeys.add(dk);
            }
        } catch (Exception ignored) {
        }

        List<ReminderItem> reminders = plan.reminders() == null ? List.of() : plan.reminders();
        List<ReminderIntentRecord> toInsert = null;
        for (ReminderItem r : reminders) {
            if (r == null) continue;
            String hint = String.valueOf(r.timeHint() == null ? "" : r.timeHint()).trim();
            if (!hint.equalsIgnoreCase(slot.name())) {
                continue;
            }
            String itemKey = String.valueOf(r.itemKey() == null ? "" : r.itemKey()).trim();
            if (itemKey.isEmpty()) continue;

            if (isCompleted(itemKey, hasMeal, hasMedicine)) {
                continue;
            }

            String windowId = date + ":" + slot.name();
            String decisionKey = "reminder:" + user.getId() + ":" + windowId + ":" + itemKey;
            if (existingDecisionKeys.contains(decisionKey)) {
                continue;
            }

            String dueTime = formatHHmm(slotStart.toLocalTime());
            ReminderIntentRecord rec = new ReminderIntentRecord();
            rec.setUserId(user.getId());
            rec.setPlanDate(date);
            rec.setTimeSlot(dueTime);
            rec.setItemKey(itemKey);
            rec.setDecisionKey(decisionKey);
            rec.setWindowId(windowId);
            rec.setValidUntil(slotEnd);
            rec.setChannel("INBOX");
            rec.setStatus("PENDING");
            rec.setExplanationCode(ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN);
            rec.setExplanationArgs(buildExplanationArgs(plan, r, slot, date));
            if (toInsert == null) toInsert = new java.util.ArrayList<>();
            toInsert.add(rec);
            existingDecisionKeys.add(decisionKey);
        }
        if (toInsert != null && !toInsert.isEmpty()) {
            try {
                reminderIntentRecordRepository.saveAll(toInsert);
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isCompleted(String itemKey, boolean hasMeal, boolean hasMedicine) {
        String key = String.valueOf(itemKey == null ? "" : itemKey).trim();
        if (key.startsWith("meal_")) {
            return hasMeal;
        }
        if (key.startsWith("medication_")) {
            return hasMedicine;
        }
        if (key.startsWith("supplement_")) {
            return hasMedicine;
        }
        return false;
    }

    private TimeSlot resolveSlot(LocalTime now) {
        if (inRange(now, morningStart, morningEnd)) return TimeSlot.MORNING;
        if (inRange(now, noonStart, noonEnd)) return TimeSlot.NOON;
        if (inRange(now, afternoonStart, afternoonEnd)) return TimeSlot.AFTERNOON;
        if (inRange(now, eveningStart, eveningEnd)) return TimeSlot.EVENING;
        if (inRange(now, nightStart, nightEnd)) return TimeSlot.NIGHT;
        return null;
    }

    private boolean inRange(LocalTime t, LocalTime start, LocalTime end) {
        return !t.isBefore(start) && !t.isAfter(end);
    }

    private LocalTime slotStart(TimeSlot slot) {
        return switch (slot) {
            case MORNING -> morningStart;
            case NOON -> noonStart;
            case AFTERNOON -> afternoonStart;
            case EVENING -> eveningStart;
            case NIGHT -> nightStart;
        };
    }

    private LocalTime slotEnd(TimeSlot slot) {
        return switch (slot) {
            case MORNING -> morningEnd;
            case NOON -> noonEnd;
            case AFTERNOON -> afternoonEnd;
            case EVENING -> eveningEnd;
            case NIGHT -> nightEnd;
        };
    }

    private String buildExplanationArgs(DailyPlan plan, ReminderItem item, TimeSlot slot, LocalDate date) {
        String planId = plan == null ? "" : String.valueOf(plan.planId() == null ? "" : plan.planId());
        String itemKey = item == null ? "" : String.valueOf(item.itemKey() == null ? "" : item.itemKey());
        String category = item == null ? "" : String.valueOf(item.category() == null ? "" : item.category());
        String timeHint = item == null ? "" : String.valueOf(item.timeHint() == null ? "" : item.timeHint());
        String slotName = slot == null ? "" : slot.name();
        String d = date == null ? "" : date.toString();
        return "{\"planId\":\"" + escape(planId) + "\",\"planDate\":\"" + escape(d) + "\",\"timeSlot\":\"" + escape(slotName) + "\",\"itemKey\":\"" + escape(itemKey) + "\",\"category\":\"" + escape(category) + "\",\"timeHint\":\"" + escape(timeHint) + "\"}";
    }

    private String escape(String s) {
        String x = String.valueOf(s == null ? "" : s);
        return x.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String formatHHmm(LocalTime t) {
        if (t == null) return "";
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}
