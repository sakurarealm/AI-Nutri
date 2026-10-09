package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.dto.req.ReminderCompleteReq;
import com.xs.ai.aiheathagent.conversation.model.DailyPlan;
import com.xs.ai.aiheathagent.conversation.model.ReminderItem;
import com.xs.ai.aiheathagent.conversation.model.TimeSlot;
import com.xs.ai.aiheathagent.conversation.service.DailyPlanService;
import com.xs.ai.aiheathagent.conversation.service.ReminderScheduleService;
import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repository.DailySummaryRecordRepository;
import com.xs.ai.aiheathagent.repository.ReminderIntentRecordRepository;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class ReminderController extends BaseController {
    private final ReminderIntentRecordRepository reminderIntentRecordRepository;
    private final UserEventRecordRepository userEventRecordRepository;
    private final ReminderScheduleService reminderScheduleService;
    private final DailyPlanService dailyPlanService;
    private final DailySummaryRecordRepository dailySummaryRecordRepository;

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

    private final LocalTime summaryTime;

    public ReminderController(
            ReminderIntentRecordRepository reminderIntentRecordRepository,
            UserEventRecordRepository userEventRecordRepository,
            ReminderScheduleService reminderScheduleService,
            DailyPlanService dailyPlanService,
            DailySummaryRecordRepository dailySummaryRecordRepository,
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
            @Value("${summary.daily.cron:0 10 22 * * *}") String summaryCron,
            UserRepo userRepo
    ) {
        super(userRepo);
        this.reminderIntentRecordRepository = reminderIntentRecordRepository;
        this.userEventRecordRepository = userEventRecordRepository;
        this.reminderScheduleService = reminderScheduleService;
        this.dailyPlanService = dailyPlanService;
        this.dailySummaryRecordRepository = dailySummaryRecordRepository;
        this.morningStart = parseLocalTime(morningStart, LocalTime.of(7, 0));
        this.morningEnd = parseLocalTime(morningEnd, LocalTime.of(9, 30));
        this.noonStart = parseLocalTime(noonStart, LocalTime.of(12, 0));
        this.noonEnd = parseLocalTime(noonEnd, LocalTime.of(13, 30));
        this.afternoonStart = parseLocalTime(afternoonStart, LocalTime.of(15, 0));
        this.afternoonEnd = parseLocalTime(afternoonEnd, LocalTime.of(17, 0));
        this.eveningStart = parseLocalTime(eveningStart, LocalTime.of(18, 0));
        this.eveningEnd = parseLocalTime(eveningEnd, LocalTime.of(20, 0));
        this.nightStart = parseLocalTime(nightStart, LocalTime.of(21, 0));
        this.nightEnd = parseLocalTime(nightEnd, LocalTime.of(22, 30));
        this.summaryTime = parseCronToTime(summaryCron, LocalTime.of(22, 10));
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

    private static LocalTime parseCronToTime(String cron, LocalTime fallback) {
        try {
            String c = String.valueOf(cron == null ? "" : cron).trim();
            if (c.isEmpty()) return fallback;
            String[] parts = c.split("\\s+");
            if (parts.length < 3) return fallback;
            int minute = Integer.parseInt(parts[1]);
            int hour = Integer.parseInt(parts[2]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return fallback;
            return LocalTime.of(hour, minute);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private LocalTime slotStartTime(String slot) {
        String s = String.valueOf(slot == null ? "" : slot).trim().toUpperCase();
        return switch (s) {
            case "MORNING" -> morningStart;
            case "NOON" -> noonStart;
            case "AFTERNOON" -> afternoonStart;
            case "EVENING" -> eveningStart;
            case "NIGHT" -> nightStart;
            default -> null;
        };
    }

    private LocalTime slotEndTime(String slot) {
        String s = String.valueOf(slot == null ? "" : slot).trim().toUpperCase();
        return switch (s) {
            case "MORNING" -> morningEnd;
            case "NOON" -> noonEnd;
            case "AFTERNOON" -> afternoonEnd;
            case "EVENING" -> eveningEnd;
            case "NIGHT" -> nightEnd;
            default -> null;
        };
    }

    private static String formatHHmm(LocalTime t) {
        if (t == null) return "";
        int h = t.getHour();
        int m = t.getMinute();
        return String.format("%02d:%02d", h, m);
    }

    private static int parseMinutes(String hhmm) {
        try {
            String s = String.valueOf(hhmm == null ? "" : hhmm).trim();
            if (!s.matches("\\d{1,2}:\\d{2}")) return Integer.MAX_VALUE;
            String[] parts = s.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) return Integer.MAX_VALUE;
            return h * 60 + m;
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static String buildPlanOccurrenceDecisionKey(Long userId, LocalDate date, String slot, String itemKey) {
        String uid = String.valueOf(userId == null ? "" : userId).trim();
        String d = String.valueOf(date == null ? "" : date).trim();
        String s = String.valueOf(slot == null ? "" : slot).trim().toUpperCase();
        String ik = String.valueOf(itemKey == null ? "" : itemKey).trim();
        if (uid.isEmpty() || d.isEmpty() || s.isEmpty() || ik.isEmpty()) return "";
        String windowId = d + ":" + s;
        return "reminder:" + uid + ":" + windowId + ":" + ik;
    }

    @GetMapping({"/api/plan/today", "/api/plan/today/", "/api/api/plan/today", "/api/api/plan/today/", "/plan/today", "/plan/today/"})
    public ApiResp<Map<String, Object>> todayPlan() {
        var user = current();
        Long userId = user.getId();
        LocalDate date = LocalDate.now();

        DailyPlan plan = dailyPlanService.findPlan(userId, date).orElseGet(() -> {
            try {
                return dailyPlanService.generateAndPersist(user, date);
            } catch (Exception ignored) {
                return null;
            }
        });

        boolean summaryExists = dailySummaryRecordRepository.existsByUserIdAndDateAndExplanationCode(userId, date, ExplanationCode.SUMMARY_SCHEDULED_DAILY);

        List<Map<String, Object>> items = new ArrayList<>();

        List<com.xs.ai.aiheathagent.entity.ReminderScheduleRecord> schedules = reminderScheduleService.listEnabledSchedules(userId);
        Map<String, ReminderIntentRecord> intentByDecisionKey = new HashMap<>();
        try {
            List<ReminderIntentRecord> intents = reminderIntentRecordRepository.findByUserIdAndPlanDate(userId, date);
            if (intents != null) {
                for (ReminderIntentRecord r : intents) {
                    if (r == null) continue;
                    String dk = String.valueOf(r.getDecisionKey() == null ? "" : r.getDecisionKey()).trim();
                    if (!dk.isEmpty()) intentByDecisionKey.put(dk, r);
                }
            }
        } catch (Exception ignored) {
        }

        for (var s : schedules) {
            if (s == null) continue;
            String scheduleKey = String.valueOf(s.getScheduleKey() == null ? "" : s.getScheduleKey()).trim();
            if (scheduleKey.isEmpty()) continue;
            String time = String.valueOf(s.getTimeSlot() == null ? "" : s.getTimeSlot()).trim();
            if (time.isEmpty()) continue;
            String decisionKey = scheduleKey + ":" + date;
            ReminderIntentRecord intent = intentByDecisionKey.get(decisionKey);
            String status = intent == null ? "SCHEDULED" : String.valueOf(intent.getStatus() == null ? "" : intent.getStatus()).trim();
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("kind", "REMINDER_SCHEDULE");
            it.put("time", time);
            it.put("title", String.valueOf(s.getSubject() == null ? "" : s.getSubject()).trim());
            it.put("itemKey", String.valueOf(s.getItemKey() == null ? "" : s.getItemKey()).trim());
            it.put("status", status);
            it.put("decisionKey", decisionKey);
            it.put("scheduleKey", scheduleKey);
            it.put("canDisable", true);
            it.put("canSkipToday", true);
            it.put("canComplete", intent != null && !"COMPLETED".equalsIgnoreCase(status));
            items.add(it);
        }

        if (plan != null && plan.reminders() != null) {
            LocalDateTime now = LocalDateTime.now();
            for (ReminderItem r : plan.reminders()) {
                if (r == null) continue;
                String slot = String.valueOf(r.timeHint() == null ? "" : r.timeHint()).trim();
                LocalTime t = slotStartTime(slot);
                if (t == null) continue;
                LocalTime te = slotEndTime(slot);
                String itemKey = String.valueOf(r.itemKey() == null ? "" : r.itemKey()).trim();
                String decisionKey = buildPlanOccurrenceDecisionKey(userId, date, slot, itemKey);
                ReminderIntentRecord intent = decisionKey.isEmpty() ? null : intentByDecisionKey.get(decisionKey);
                String status = intent == null ? "PLANNED" : String.valueOf(intent.getStatus() == null ? "" : intent.getStatus()).trim();
                boolean completedByEvent = false;
                try {
                    if (te != null) {
                        LocalDateTime start = date.atTime(t);
                        LocalDateTime end = date.atTime(te);
                        String ik = String.valueOf(itemKey == null ? "" : itemKey).trim();
                        String eventType = "";
                        if (ik.startsWith("meal_")) eventType = "MEAL";
                        else if (ik.startsWith("exercise_")) eventType = "EXERCISE";
                        else if (ik.startsWith("medication_") || ik.startsWith("supplement_")) eventType = "MEDICINE";
                        if (!eventType.isEmpty()) {
                            long cnt = userEventRecordRepository.countByUserIdAndEventTypeAndOccurredAtBetween(userId, eventType, start, end);
                            completedByEvent = cnt > 0;
                        }
                    }
                } catch (Exception ignored) {
                }
                if (completedByEvent) status = "COMPLETED";
                boolean inWindow = false;
                try {
                    if (te != null) {
                        LocalDateTime start = date.atTime(t);
                        LocalDateTime end = date.atTime(te);
                        inWindow = !now.isBefore(start) && !now.isAfter(end);
                    }
                } catch (Exception ignored) {
                }
                boolean canComplete = inWindow
                        && !"COMPLETED".equalsIgnoreCase(status)
                        && !"EXPIRED".equalsIgnoreCase(status)
                        && !"SKIPPED".equalsIgnoreCase(status)
                        && !"DISABLED".equalsIgnoreCase(status);
                Map<String, Object> it = new LinkedHashMap<>();
                it.put("kind", "PLAN_REMINDER");
                it.put("time", formatHHmm(t));
                it.put("title", String.valueOf(r.title() == null ? "" : r.title()).trim());
                it.put("itemKey", itemKey);
                it.put("status", status);
                it.put("timeHint", slot);
                it.put("decisionKey", decisionKey);
                it.put("canDisable", false);
                it.put("canSkipToday", false);
                it.put("canComplete", canComplete);
                items.add(it);
            }
        }

        Map<String, Object> summaryItem = new LinkedHashMap<>();
        summaryItem.put("kind", "SUMMARY");
        summaryItem.put("time", formatHHmm(summaryTime));
        summaryItem.put("title", "日终汇总");
        summaryItem.put("status", summaryExists ? "DONE" : "PLANNED");
        summaryItem.put("canDisable", false);
        summaryItem.put("canSkipToday", false);
        summaryItem.put("canComplete", false);
        items.add(summaryItem);

        items.sort((a, b) -> Integer.compare(parseMinutes(String.valueOf(a.get("time"))), parseMinutes(String.valueOf(b.get("time")))));

        Map<String, Object> planView = new LinkedHashMap<>();
        if (plan != null) {
            planView.put("planDate", plan.planDate() == null ? "" : plan.planDate().toString());
            planView.put("passiveOnly", plan.passiveOnly());
            planView.put("variationFlag", plan.variationFlag());
            planView.put("breakfast", plan.breakfast());
            planView.put("lunch", plan.lunch());
            planView.put("dinner", plan.dinner());
            planView.put("exercise", plan.exercise());
        }

        Map<String, Object> slotRanges = new LinkedHashMap<>();
        slotRanges.put(TimeSlot.MORNING.name(), Map.of("start", formatHHmm(morningStart)));
        slotRanges.put(TimeSlot.NOON.name(), Map.of("start", formatHHmm(noonStart)));
        slotRanges.put(TimeSlot.AFTERNOON.name(), Map.of("start", formatHHmm(afternoonStart)));
        slotRanges.put(TimeSlot.EVENING.name(), Map.of("start", formatHHmm(eveningStart)));
        slotRanges.put(TimeSlot.NIGHT.name(), Map.of("start", formatHHmm(nightStart)));

        return ApiResp.ok(Map.of(
                "date", date.toString(),
                "plan", planView,
                "slotRanges", slotRanges,
                "items", items
        ));
    }

    @PostMapping({"/api/reminder/complete", "/api/reminder/complete/", "/api/api/reminder/complete", "/api/api/reminder/complete/", "/reminder/complete", "/reminder/complete/"})
    public ApiResp<Map<String, Object>> complete(@RequestBody ReminderCompleteReq req) {
        var user = current();
        Long userId = user.getId();
        String decisionKey = String.valueOf(req == null ? "" : (req.getDecisionKey() == null ? "" : req.getDecisionKey())).trim();
        if (decisionKey.isEmpty()) {
            return ApiResp.fail(400, "decisionKey required");
        }

        ReminderIntentRecord intent = reminderIntentRecordRepository
                .findTopByUserIdAndDecisionKeyOrderByCreatedAtDescIdDesc(userId, decisionKey)
                .orElse(null);
        if (intent == null) {
            LocalDateTime now = LocalDateTime.now();
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("^reminder:(\\d+):(\\d{4}-\\d{2}-\\d{2}):(MORNING|NOON|AFTERNOON|EVENING|NIGHT):(.+)$")
                        .matcher(decisionKey);
                if (m.find()) {
                    Long uid = Long.parseLong(m.group(1));
                    if (uid.equals(userId)) {
                        LocalDate d = LocalDate.parse(m.group(2));
                        String slot = String.valueOf(m.group(3) == null ? "" : m.group(3)).trim().toUpperCase();
                        String ik = String.valueOf(m.group(4) == null ? "" : m.group(4)).trim();
                        LocalTime st = slotStartTime(slot);
                        LocalTime et = slotEndTime(slot);
                        if (st != null && et != null && !ik.isEmpty()) {
                            ReminderIntentRecord rec = new ReminderIntentRecord();
                            rec.setUserId(userId);
                            rec.setPlanDate(d);
                            rec.setTimeSlot(formatHHmm(st));
                            rec.setItemKey(ik);
                            rec.setDecisionKey(decisionKey);
                            rec.setWindowId(d + ":" + slot);
                            rec.setValidUntil(d.atTime(et));
                            rec.setChannel("INBOX");
                            rec.setStatus("COMPLETED");
                            rec.setExplanationCode(ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN);
                            rec.setExplanationArgs("{\"action\":\"MANUAL_COMPLETE\",\"planDate\":\"" + d + "\",\"timeSlot\":\"" + slot + "\",\"itemKey\":\"" + ik + "\"}");
                            rec.setExecutedAt(now);
                            reminderIntentRecordRepository.save(rec);
                        }
                    }
                } else {
                    int idx = decisionKey.lastIndexOf(':');
                    if (idx > 0 && idx < decisionKey.length() - 1) {
                        String tail = decisionKey.substring(idx + 1);
                        if (tail.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            LocalDate d = LocalDate.parse(tail);
                            String scheduleKey = decisionKey.substring(0, idx);
                            String[] parts = scheduleKey.split(":");
                            if (parts.length >= 4 && "reminder".equals(parts[0]) && String.valueOf(userId).equals(parts[1])) {
                                String scheduleTime = String.valueOf(parts[parts.length - 1]).trim();
                                String normalizedTime = reminderScheduleService.normalizeTimeSlot(scheduleTime);
                                StringBuilder sb = new StringBuilder();
                                for (int i = 2; i < parts.length - 1; i++) {
                                    if (i > 2) sb.append(':');
                                    sb.append(parts[i]);
                                }
                                String ik = sb.toString().trim();
                                if (!ik.isEmpty() && !normalizedTime.isEmpty()) {
                                    ReminderIntentRecord rec = new ReminderIntentRecord();
                                    rec.setUserId(userId);
                                    rec.setPlanDate(d);
                                    rec.setTimeSlot(normalizedTime);
                                    rec.setItemKey(ik);
                                    rec.setDecisionKey(decisionKey);
                                    rec.setWindowId(d + ":" + normalizedTime);
                                    LocalTime lt = parseLocalTime(normalizedTime, LocalTime.of(9, 0));
                                    rec.setValidUntil(d.atTime(lt).plusHours(8));
                                    rec.setChannel("INBOX");
                                    rec.setStatus("COMPLETED");
                                    rec.setExplanationCode(ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN);
                                    rec.setExplanationArgs("{\"action\":\"MANUAL_COMPLETE\",\"scheduleKey\":\"" + scheduleKey + "\",\"planDate\":\"" + d + "\",\"timeSlot\":\"" + normalizedTime + "\",\"itemKey\":\"" + ik + "\"}");
                                    rec.setExecutedAt(now);
                                    reminderIntentRecordRepository.save(rec);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            intent = reminderIntentRecordRepository
                    .findTopByUserIdAndDecisionKeyOrderByCreatedAtDescIdDesc(userId, decisionKey)
                    .orElse(null);
        }
        if (intent == null) {
            return ApiResp.fail(404, "reminder intent not found");
        }

        LocalDateTime now = LocalDateTime.now();

        String itemKey = String.valueOf(intent.getItemKey() == null ? "" : intent.getItemKey()).trim();
        String eventType = "MEDICINE";
        if (itemKey.startsWith("meal_")) {
            eventType = "MEAL";
        } else if (itemKey.startsWith("exercise_")) {
            eventType = "EXERCISE";
        }

        UserEventRecord ev = new UserEventRecord();
        ev.setUserId(userId);
        ev.setSessionId("reminder_complete_" + userId);
        ev.setOccurredAt(now);
        ev.setModality("MANUAL");
        ev.setEventType(eventType);
        ev.setTopics("");
        ev.setConfidence(1.0);
        ev.setSuggestOpenCamera(false);
        ev.setSuggestWriteNutrition(false);
        ev.setContentText("complete:" + decisionKey);
        userEventRecordRepository.save(ev);

        try {
            intent.setStatus("COMPLETED");
            intent.setExecutedAt(now);
            reminderIntentRecordRepository.save(intent);
        } catch (Exception ignored) {
        }

        return ApiResp.ok(Map.of(
                "ok", true,
                "decisionKey", decisionKey,
                "eventId", ev.getId(),
                "completedAt", now.toString()
        ));
    }

    @PostMapping({"/api/reminder/skip-today", "/api/reminder/skip-today/", "/api/api/reminder/skip-today", "/api/api/reminder/skip-today/", "/reminder/skip-today", "/reminder/skip-today/"})
    public ApiResp<Map<String, Object>> skipToday(@RequestBody ReminderCompleteReq req) {
        var user = current();
        Long userId = user.getId();
        String decisionKey = String.valueOf(req == null ? "" : (req.getDecisionKey() == null ? "" : req.getDecisionKey())).trim();
        if (decisionKey.isEmpty()) {
            return ApiResp.fail(400, "decisionKey required");
        }
        String scheduleKey = reminderScheduleService.tryExtractScheduleKey(decisionKey).orElse("");
        if (scheduleKey.isEmpty()) {
            return ApiResp.fail(400, "invalid decisionKey");
        }
        boolean ok = reminderScheduleService.skipTodayByScheduleKey(userId, scheduleKey, LocalDate.now());
        return ApiResp.ok(Map.of("ok", ok, "scheduleKey", scheduleKey));
    }

    @PostMapping({"/api/reminder/schedule/disable", "/api/reminder/schedule/disable/", "/api/api/reminder/schedule/disable", "/api/api/reminder/schedule/disable/", "/reminder/schedule/disable", "/reminder/schedule/disable/"})
    public ApiResp<Map<String, Object>> disableSchedule(@RequestBody ReminderCompleteReq req) {
        var user = current();
        Long userId = user.getId();
        String decisionKey = String.valueOf(req == null ? "" : (req.getDecisionKey() == null ? "" : req.getDecisionKey())).trim();
        if (decisionKey.isEmpty()) {
            return ApiResp.fail(400, "decisionKey required");
        }
        String scheduleKey = reminderScheduleService.tryExtractScheduleKey(decisionKey).orElse("");
        if (scheduleKey.isEmpty()) {
            return ApiResp.fail(400, "invalid decisionKey");
        }
        boolean ok = reminderScheduleService.disableByScheduleKey(userId, scheduleKey);
        try {
            reminderScheduleService.skipTodayByScheduleKey(userId, scheduleKey, LocalDate.now());
        } catch (Exception ignored) {
        }
        return ApiResp.ok(Map.of("ok", ok, "scheduleKey", scheduleKey));
    }
}
