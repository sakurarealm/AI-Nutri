package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.entity.ReminderScheduleRecord;
import com.xs.ai.aiheathagent.repository.ReminderIntentRecordRepository;
import com.xs.ai.aiheathagent.repository.ReminderScheduleRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ReminderScheduleService {
    private final ReminderScheduleRecordRepository scheduleRepository;
    private final ReminderIntentRecordRepository intentRepository;

    private final LocalTime morningStart;
    private final LocalTime noonStart;
    private final LocalTime afternoonStart;
    private final LocalTime eveningStart;
    private final LocalTime nightStart;

    private final int quietStartHour;
    private final int quietEndHour;

    private final Object enabledScheduleCacheLock = new Object();
    private volatile List<ReminderScheduleRecord> enabledScheduleCache = null;
    private volatile LocalDateTime enabledScheduleCacheAt = null;

    public ReminderScheduleService(
            ReminderScheduleRecordRepository scheduleRepository,
            ReminderIntentRecordRepository intentRepository,
            @Value("${timeslot.morning.start:07:00}") String morningStart,
            @Value("${timeslot.noon.start:12:00}") String noonStart,
            @Value("${timeslot.afternoon.start:15:00}") String afternoonStart,
            @Value("${timeslot.evening.start:18:00}") String eveningStart,
            @Value("${timeslot.night.start:21:00}") String nightStart,
            @Value("${reminder.quiet-start-hour:23}") int quietStartHour,
            @Value("${reminder.quiet-end-hour:7}") int quietEndHour
    ) {
        this.scheduleRepository = scheduleRepository;
        this.intentRepository = intentRepository;
        this.morningStart = parseLocalTime(morningStart, LocalTime.of(7, 0));
        this.noonStart = parseLocalTime(noonStart, LocalTime.of(12, 0));
        this.afternoonStart = parseLocalTime(afternoonStart, LocalTime.of(15, 0));
        this.eveningStart = parseLocalTime(eveningStart, LocalTime.of(18, 0));
        this.nightStart = parseLocalTime(nightStart, LocalTime.of(21, 0));
        this.quietStartHour = quietStartHour;
        this.quietEndHour = quietEndHour;
    }

    private void clearEnabledScheduleCache() {
        synchronized (enabledScheduleCacheLock) {
            enabledScheduleCache = null;
            enabledScheduleCacheAt = null;
        }
    }

    public String normalizeTimeSlot(String raw) {
        return normalizeScheduleTime(raw);
    }

    @Transactional
    public ReminderScheduleRecord upsertSchedule(Long userId, String scheduleKey, String scheduleTime, String itemKey, String subject) {
        if (userId == null) throw new IllegalArgumentException("userId required");
        String sk = String.valueOf(scheduleKey == null ? "" : scheduleKey).trim();
        String t = normalizeScheduleTime(scheduleTime);
        String ik = String.valueOf(itemKey == null ? "" : itemKey).trim();
        String sub = String.valueOf(subject == null ? "" : subject).trim();
        if (sk.isEmpty() || t.isEmpty() || ik.isEmpty()) throw new IllegalArgumentException("scheduleKey/time/itemKey required");
        ReminderScheduleRecord rec = scheduleRepository.findByUserIdAndScheduleKey(userId, sk).orElseGet(() -> {
            ReminderScheduleRecord r = new ReminderScheduleRecord();
            r.setUserId(userId);
            r.setScheduleKey(sk);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });
        rec.setTimeSlot(t);
        rec.setItemKey(ik);
        rec.setSubject(sub.isEmpty() ? ik : sub);
        rec.setEnabled(true);
        rec.setUpdatedAt(LocalDateTime.now());
        ReminderScheduleRecord out = scheduleRepository.save(rec);
        clearEnabledScheduleCache();
        return out;
    }

    public List<ReminderScheduleRecord> listEnabledSchedules(Long userId) {
        if (userId == null) return List.of();
        return scheduleRepository.findByUserIdAndEnabledTrue(userId);
    }

    public List<ReminderScheduleRecord> listAllEnabledSchedules() {
        return scheduleRepository.findByEnabledTrue();
    }

    private List<ReminderScheduleRecord> listAllEnabledSchedulesCached(LocalDateTime now) {
        LocalDateTime at = now == null ? LocalDateTime.now() : now;
        List<ReminderScheduleRecord> cached = enabledScheduleCache;
        LocalDateTime cachedAt = enabledScheduleCacheAt;
        if (cached != null && cachedAt != null && cachedAt.isAfter(at.minusMinutes(5))) {
            return cached;
        }
        synchronized (enabledScheduleCacheLock) {
            cached = enabledScheduleCache;
            cachedAt = enabledScheduleCacheAt;
            if (cached != null && cachedAt != null && cachedAt.isAfter(at.minusMinutes(5))) {
                return cached;
            }
            List<ReminderScheduleRecord> fresh = scheduleRepository.findByEnabledTrue();
            enabledScheduleCache = fresh;
            enabledScheduleCacheAt = at;
            return fresh;
        }
    }

    @Transactional
    public boolean disableByScheduleKey(Long userId, String scheduleKey) {
        if (userId == null) return false;
        String sk = String.valueOf(scheduleKey == null ? "" : scheduleKey).trim();
        if (sk.isEmpty()) return false;
        ReminderScheduleRecord rec = scheduleRepository.findByUserIdAndScheduleKey(userId, sk).orElse(null);
        if (rec == null) return false;
        rec.setEnabled(false);
        rec.setUpdatedAt(LocalDateTime.now());
        scheduleRepository.save(rec);
        clearEnabledScheduleCache();
        return true;
    }

    @Transactional
    public int disableMatching(Long userId, String subject, String scheduleTime, String prefix) {
        if (userId == null) return 0;
        String sub = String.valueOf(subject == null ? "" : subject).trim();
        String time = String.valueOf(scheduleTime == null ? "" : scheduleTime).trim();
        String pfx = String.valueOf(prefix == null ? "" : prefix).trim();
        List<ReminderScheduleRecord> list = scheduleRepository.findByUserIdAndEnabledTrue(userId);
        int n = 0;
        for (ReminderScheduleRecord s : list) {
            if (s == null) continue;
            if (!time.isEmpty() && !time.equals(String.valueOf(s.getTimeSlot() == null ? "" : s.getTimeSlot()).trim())) continue;
            if (!sub.isEmpty()) {
                String sk = String.valueOf(s.getScheduleKey() == null ? "" : s.getScheduleKey());
                String ik = String.valueOf(s.getItemKey() == null ? "" : s.getItemKey());
                String sj = String.valueOf(s.getSubject() == null ? "" : s.getSubject());
                if (sk.indexOf(sub) < 0 && ik.indexOf(sub) < 0 && sj.indexOf(sub) < 0) continue;
            }
            if (!pfx.isEmpty()) {
                String ik = String.valueOf(s.getItemKey() == null ? "" : s.getItemKey()).trim();
                if (!ik.startsWith(pfx)) continue;
            }
            s.setEnabled(false);
            s.setUpdatedAt(LocalDateTime.now());
            scheduleRepository.save(s);
            n++;
        }
        if (n > 0) {
            clearEnabledScheduleCache();
        }
        return n;
    }

    @Transactional
    public boolean skipTodayByScheduleKey(Long userId, String scheduleKey, LocalDate today) {
        if (userId == null) return false;
        LocalDate d = today == null ? LocalDate.now() : today;
        String sk = String.valueOf(scheduleKey == null ? "" : scheduleKey).trim();
        if (sk.isEmpty()) return false;

        ReminderScheduleRecord schedule = scheduleRepository.findByUserIdAndScheduleKey(userId, sk).orElse(null);
        if (schedule == null) return false;

        String scheduleTime = normalizeScheduleTime(schedule.getTimeSlot());
        String occurrenceKey = buildOccurrenceKey(sk, d);
        if (!intentRepository.existsByUserIdAndDecisionKey(userId, occurrenceKey)) {
            ReminderIntentRecord rec = new ReminderIntentRecord();
            rec.setUserId(userId);
            rec.setPlanDate(d);
            rec.setTimeSlot(scheduleTime);
            rec.setItemKey(schedule.getItemKey());
            rec.setDecisionKey(occurrenceKey);
            rec.setWindowId(d + ":" + scheduleTime);
            LocalDateTime due = dueAt(d, scheduleTime);
            rec.setValidUntil(due.plusHours(8));
            rec.setChannel("INBOX");
            rec.setStatus("SKIPPED");
            rec.setExplanationCode(ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN);
            rec.setExplanationArgs("{\"scheduleKey\":\"" + escape(sk) + "\",\"planDate\":\"" + escape(d.toString()) + "\",\"timeSlot\":\"" + escape(scheduleTime) + "\",\"itemKey\":\"" + escape(schedule.getItemKey()) + "\",\"subject\":\"" + escape(schedule.getSubject()) + "\",\"action\":\"SKIP_TODAY\"}");
            rec.setExecutedAt(LocalDateTime.now());
            intentRepository.save(rec);
            return true;
        }
        return intentRepository.findTopByUserIdAndDecisionKeyOrderByCreatedAtDescIdDesc(userId, occurrenceKey)
                .map(intent -> {
                    intent.setStatus("SKIPPED");
                    intent.setExecutedAt(LocalDateTime.now());
                    intentRepository.save(intent);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void materializeDueSchedules(LocalDateTime now, int horizonMinutes) {
        LocalDateTime at = now == null ? LocalDateTime.now() : now;
        int horizon = Math.max(1, Math.min(horizonMinutes, 180));
        LocalDate today = at.toLocalDate();
        LocalDateTime latest = at.plusMinutes(horizon);
        List<ReminderScheduleRecord> schedules = listAllEnabledSchedulesCached(at);
        Map<Long, List<ReminderScheduleRecord>> byUserId = new HashMap<>();
        List<ReminderScheduleRecord> toUpdate = new ArrayList<>();
        LocalDateTime updateAt = LocalDateTime.now();
        for (ReminderScheduleRecord s : schedules) {
            if (s == null || !s.isEnabled()) continue;
            Long userId = s.getUserId();
            if (userId == null) continue;
            String scheduleTime = normalizeScheduleTime(s.getTimeSlot());
            if (!scheduleTime.isEmpty() && !scheduleTime.equals(String.valueOf(s.getTimeSlot() == null ? "" : s.getTimeSlot()).trim())) {
                try {
                    s.setTimeSlot(scheduleTime);
                    s.setUpdatedAt(updateAt);
                    toUpdate.add(s);
                } catch (Exception ignored) {
                }
            }
            byUserId.computeIfAbsent(userId, k -> new ArrayList<>()).add(s);
        }
        if (!toUpdate.isEmpty()) {
            try {
                scheduleRepository.saveAll(toUpdate);
            } catch (Exception ignored) {
            }
            clearEnabledScheduleCache();
        }

        for (Map.Entry<Long, List<ReminderScheduleRecord>> e : byUserId.entrySet()) {
            Long userId = e.getKey();
            if (userId == null) continue;
            Set<String> existing = new HashSet<>();
            try {
                List<ReminderIntentRecord> existingIntents = intentRepository.findByUserIdAndPlanDate(userId, today);
                for (ReminderIntentRecord r : existingIntents) {
                    if (r == null) continue;
                    String dk = String.valueOf(r.getDecisionKey() == null ? "" : r.getDecisionKey()).trim();
                    if (!dk.isEmpty()) existing.add(dk);
                }
            } catch (Exception ignored) {
            }

            for (ReminderScheduleRecord s : e.getValue()) {
                if (s == null || !s.isEnabled()) continue;
                String scheduleTime = normalizeScheduleTime(s.getTimeSlot());
                LocalDateTime due = dueAt(today, scheduleTime);
                if (due.isAfter(latest)) continue;
                if (due.isBefore(at.minusMinutes(120))) continue;

                String scheduleKey = String.valueOf(s.getScheduleKey() == null ? "" : s.getScheduleKey()).trim();
                if (scheduleKey.isEmpty()) continue;
                String occurrenceKey = buildOccurrenceKey(scheduleKey, today);
                if (existing.contains(occurrenceKey)) continue;

                ReminderIntentRecord rec = new ReminderIntentRecord();
                rec.setUserId(userId);
                rec.setPlanDate(today);
                rec.setTimeSlot(scheduleTime);
                rec.setItemKey(s.getItemKey());
                rec.setDecisionKey(occurrenceKey);
                rec.setWindowId(today + ":" + scheduleTime);
                rec.setValidUntil(due.plusHours(8));
                rec.setChannel("INBOX");
                rec.setStatus("PENDING");
                rec.setExplanationCode(ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN);
                rec.setExplanationArgs("{\"scheduleKey\":\"" + escape(scheduleKey) + "\",\"planDate\":\"" + escape(today.toString()) + "\",\"timeSlot\":\"" + escape(scheduleTime) + "\",\"itemKey\":\"" + escape(s.getItemKey()) + "\",\"subject\":\"" + escape(s.getSubject()) + "\"}");
                try {
                    intentRepository.save(rec);
                    existing.add(occurrenceKey);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public String buildOccurrenceKey(String scheduleKey, LocalDate date) {
        String sk = String.valueOf(scheduleKey == null ? "" : scheduleKey).trim();
        String d = date == null ? "" : date.toString();
        return sk + ":" + d;
    }

    public Optional<String> tryExtractScheduleKey(String decisionKey) {
        String dk = String.valueOf(decisionKey == null ? "" : decisionKey).trim();
        if (dk.isEmpty()) return Optional.empty();
        int idx = dk.lastIndexOf(':');
        if (idx <= 0 || idx >= dk.length() - 1) return Optional.empty();
        String tail = dk.substring(idx + 1);
        if (tail.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return Optional.of(dk.substring(0, idx));
        }
        return Optional.of(dk);
    }

    private LocalDateTime dueAt(LocalDate date, String scheduleTime) {
        LocalDate d = date == null ? LocalDate.now() : date;
        LocalTime lt = parseTimeSlot(scheduleTime);
        if (lt == null) lt = LocalTime.of(9, 0);
        return d.atTime(lt);
    }

    private String normalizeScheduleTime(String scheduleTime) {
        String t = String.valueOf(scheduleTime == null ? "" : scheduleTime).trim();
        if (t.isEmpty()) return "";
        LocalTime lt = parseTimeSlot(t);
        if (lt == null) return t;
        return formatHHmm(lt);
    }

    private LocalTime parseTimeSlot(String scheduleTime) {
        String t = String.valueOf(scheduleTime == null ? "" : scheduleTime).trim();
        if (t.isEmpty()) return null;
        if (t.matches("\\d{1,2}:\\d{1,2}(:\\d{1,2})?")) {
            try {
                LocalTime lt = LocalTime.parse(t.length() == 4 ? ("0" + t) : t);
                return clampQuietHours(lt);
            } catch (Exception ignored) {
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
        if (base == null) return null;
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

    private static String formatHHmm(LocalTime t) {
        if (t == null) return "";
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private String escape(String s) {
        String v = String.valueOf(s == null ? "" : s);
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
