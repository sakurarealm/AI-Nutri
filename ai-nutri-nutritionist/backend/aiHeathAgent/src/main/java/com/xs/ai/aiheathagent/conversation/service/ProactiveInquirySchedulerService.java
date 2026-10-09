package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.client.DeepSeekChatClient;
import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.conversation.expose.SystemMessageType;
import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import com.xs.ai.aiheathagent.conversation.model.ConversationRole;
import com.xs.ai.aiheathagent.conversation.model.DecisionActionType;
import com.xs.ai.aiheathagent.conversation.model.DecisionResult;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.prompt.Prompt;
import com.xs.ai.aiheathagent.conversation.prompt.PromptProvider;
import com.xs.ai.aiheathagent.conversation.prompt.PromptRole;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.SystemMessageMetaRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ProactiveInquirySchedulerService {
    private final UserRepo userRepo;
    private final ConversationRecordRepository conversationRecordRepository;
    private final ProactiveTouchService proactiveTouchService;
    private final PromptProvider promptProvider;
    private final DeepSeekChatClient deepSeekChatClient;
    private final SystemMessageMetaRecordRepository systemMessageMetaRecordRepository;

    private final boolean enabled;
    private final boolean llmEnabled;
    private final int onlyLoginWithinDays;
    private final int quietStartHour;
    private final int quietEndHour;
    private final int minGapMinutes;
    private final int engagedCooldownMinutes;
    private final int recentUserCooldownMinutes;
    private final int maxPerDay;
    private final int lookbackDays;
    private final double includeContextProb;
    private final int scanPageSize;

    public ProactiveInquirySchedulerService(
            UserRepo userRepo,
            ConversationRecordRepository conversationRecordRepository,
            ProactiveTouchService proactiveTouchService,
            PromptProvider promptProvider,
            DeepSeekChatClient deepSeekChatClient,
            SystemMessageMetaRecordRepository systemMessageMetaRecordRepository,
            @Value("${inquiry.proactive.enabled:true}") boolean enabled,
            @Value("${inquiry.proactive.llm-enabled:false}") boolean llmEnabled,
            @Value("${inquiry.proactive.only-login-within-days:30}") int onlyLoginWithinDays,
            @Value("${inquiry.proactive.quiet-start-hour:23}") int quietStartHour,
            @Value("${inquiry.proactive.quiet-end-hour:7}") int quietEndHour,
            @Value("${inquiry.proactive.min-gap-minutes:240}") int minGapMinutes,
            @Value("${inquiry.proactive.engaged-cooldown-minutes:360}") int engagedCooldownMinutes,
            @Value("${inquiry.proactive.recent-user-cooldown-minutes:60}") int recentUserCooldownMinutes,
            @Value("${inquiry.proactive.max-per-day:2}") int maxPerDay,
            @Value("${inquiry.proactive.lookback-days:7}") int lookbackDays,
            @Value("${inquiry.proactive.include-context-prob:0.35}") double includeContextProb,
            @Value("${inquiry.proactive.scan-page-size:300}") int scanPageSize
    ) {
        this.userRepo = userRepo;
        this.conversationRecordRepository = conversationRecordRepository;
        this.proactiveTouchService = proactiveTouchService;
        this.promptProvider = promptProvider;
        this.deepSeekChatClient = deepSeekChatClient;
        this.systemMessageMetaRecordRepository = systemMessageMetaRecordRepository;
        this.enabled = enabled;
        this.llmEnabled = llmEnabled;
        this.onlyLoginWithinDays = onlyLoginWithinDays;
        this.quietStartHour = quietStartHour;
        this.quietEndHour = quietEndHour;
        this.minGapMinutes = minGapMinutes;
        this.engagedCooldownMinutes = engagedCooldownMinutes;
        this.recentUserCooldownMinutes = recentUserCooldownMinutes;
        this.maxPerDay = maxPerDay;
        this.lookbackDays = lookbackDays;
        this.includeContextProb = includeContextProb;
        this.scanPageSize = Math.max(20, scanPageSize);
    }

    @Scheduled(fixedDelayString = "${inquiry.proactive.tick-interval-ms:300000}")
    public void tick() {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (inQuietHours(now)) {
            return;
        }
        LocalDateTime winStart = windowStart(now);
        String winId = windowId(winStart);
        LocalDateTime after = now.minusDays(Math.max(0, onlyLoginWithinDays));
        int page = 0;
        while (true) {
            Page<User> users;
            try {
                users = userRepo.findActiveUsersLoggedInAfter(after, PageRequest.of(page, scanPageSize));
            } catch (Exception e) {
                users = Page.empty();
            }
            if (users == null || users.isEmpty()) break;
            for (User u : users.getContent()) {
                try {
                    if (!shouldProactiveForUser(u, now)) continue;
                    if (!shouldAttemptWindow(u, winStart)) continue;
                    if (!canSend(u, now)) continue;
                    pushOne(u, now, winStart, winId);
                } catch (Exception ignored) {
                }
            }
            if (!users.hasNext()) break;
            page++;
        }
    }

    private boolean canSend(User user, LocalDateTime now) {
        if (user == null || user.getId() == null) return false;
        if (user.getStatus() != null && user.getStatus() == 0) return false;

        Long userId = user.getId();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        try {
            long sentToday = systemMessageMetaRecordRepository.countByUserIdAndSystemMessageTypeAndCreatedAtGreaterThanEqual(
                    userId,
                    SystemMessageType.PROACTIVE_INQUIRY,
                    todayStart
            );
            if (sentToday >= Math.max(0, maxPerDay)) {
                return false;
            }
        } catch (Exception ignored) {
        }

        LocalDateTime lastUserAt = null;
        try {
            String inboxSessionId = "inbox_" + userId;
            var lastUserMsg = conversationRecordRepository
                    .findTopByUserIdAndMessageTypeAndSessionIdNotOrderByTimestampDescIdDesc(
                            userId,
                            ConversationRecord.MessageType.USER,
                            inboxSessionId
                    )
                    .orElse(null);
            if (lastUserMsg != null) {
                lastUserAt = lastUserMsg.getTimestamp();
            }
        } catch (Exception ignored) {
        }
        if (lastUserAt != null) {
            long sinceUser = ChronoUnit.MINUTES.between(lastUserAt, now);
            if (sinceUser >= 0 && sinceUser < Math.max(1, recentUserCooldownMinutes)) {
                return false;
            }
        }

        LocalDateTime lastInquiryAt = null;
        try {
            var last = systemMessageMetaRecordRepository
                    .findTopByUserIdAndSystemMessageTypeOrderByCreatedAtDescIdDesc(userId, SystemMessageType.PROACTIVE_INQUIRY)
                    .orElse(null);
            if (last != null) {
                lastInquiryAt = last.getCreatedAt();
            }
        } catch (Exception ignored) {
        }
        if (lastInquiryAt == null) {
            return true;
        }

        boolean engaged = lastUserAt != null && lastUserAt.isAfter(lastInquiryAt);
        int target = engaged ? Math.max(1, engagedCooldownMinutes) : Math.max(1, minGapMinutes);
        long sinceInquiry = ChronoUnit.MINUTES.between(lastInquiryAt, now);
        return sinceInquiry >= target;
    }

    private void pushOne(User user, LocalDateTime now, LocalDateTime winStart, String winId) {
        Long userId = user.getId();
        String windowId = "inquiry:" + winId;
        String decisionKey = "inquiry:" + userId + ":" + winId;
        DecisionResult decision = new DecisionResult(
                userId,
                decisionKey,
                windowId,
                winStart.plusMinutes(windowMinutes()),
                EnumSet.of(DecisionActionType.PROACTIVE_INBOX),
                false
        );
        String text = compose(user, now, winStart);
        if (text.isEmpty()) return;
        String args = buildExplanationArgs(now, winStart, text);
        proactiveTouchService.executeInboxTouch(user, decision, MessageContent.text(text), ExplanationCode.PROACTIVE_INQUIRY_SCHEDULED, args);
    }

    private String compose(User user, LocalDateTime now, LocalDateTime winStart) {
        String text = template(user, now);
        if (!llmEnabled) {
            return String.valueOf(text == null ? "" : text).trim();
        }
        if (deepSeekChatClient == null || !deepSeekChatClient.isConfigured()) {
            return String.valueOf(text == null ? "" : text).trim();
        }
        Long userId = user == null ? null : user.getId();
        boolean includeContext = shouldIncludeContext(userId, winStart);
        List<String> quotes = includeContext ? loadRecentUserQuotes(userId, now) : List.of();
        try {
            Prompt p = promptProvider.get(PromptRole.PROACTIVE_INQUIRY);
            String sys = p == null ? "" : String.valueOf(p.systemText() == null ? "" : p.systemText()).trim();
            String userMsg = buildUserMessage(user, now, quotes);
            List<ConversationMessage> messages = new ArrayList<>();
            if (!sys.isEmpty()) {
                messages.add(new ConversationMessage(ConversationRole.SYSTEM, MessageContent.text(sys), LocalDateTime.now()));
            }
            messages.add(new ConversationMessage(ConversationRole.USER, MessageContent.text(userMsg), LocalDateTime.now()));
            String out = String.valueOf(deepSeekChatClient.lightComplete(messages)).trim();
            if (!out.isEmpty()) {
                text = out;
            }
        } catch (Exception ignored) {
        }
        return String.valueOf(text == null ? "" : text).trim();
    }

    private String buildUserMessage(User user, LocalDateTime now, List<String> quotes) {
        String name = user == null ? "" : String.valueOf(user.getNickname() == null ? "" : user.getNickname()).trim();
        String prefix = name.isEmpty() ? "" : (name + "，");
        StringBuilder sb = new StringBuilder();
        sb.append(prefix)
                .append("上下文：now=").append(now)
                .append(", weekday=").append(toCnWeekday(now.getDayOfWeek()))
                .append(", timeLabel=").append(timeLabel(now))
                .append("。");
        if (quotes != null && !quotes.isEmpty()) {
            sb.append("用户最近聊天片段（截断）：");
            for (int i = 0; i < quotes.size(); i++) {
                String q = String.valueOf(quotes.get(i) == null ? "" : quotes.get(i)).trim();
                if (!q.isEmpty()) {
                    sb.append(i == 0 ? "" : "；").append("“").append(q).append("”");
                }
            }
            sb.append("。");
        }
        sb.append("请像真人一样自然地发起一次主动问讯：只输出一条消息；一次只问一个关键点；尽量给 2-3 个可选项让用户用数字回复；避免机械寒暄。");
        return sb.toString();
    }

    private String template(User user, LocalDateTime now) {
        String name = user == null ? "" : String.valueOf(user.getNickname() == null ? "" : user.getNickname()).trim();
        String prefix = name.isEmpty() ? "" : (name + "，");
        String label = timeLabel(now);
        if ("早上".equals(label) || "上午".equals(label)) {
            return prefix + "早呀，你今天早餐想吃清爽一点还是扎实一点？我帮你配一份。";
        }
        if ("中午".equals(label)) {
            return prefix + "到中午啦，你午饭打算怎么解决：外卖还是自己做？我给你一条更省心的选法。";
        }
        if ("下午".equals(label)) {
            return prefix + "下午这会儿最容易犯困，你现在更需要：提神一点，还是想吃点不容易胖的加餐？";
        }
        if ("晚上".equals(label)) {
            return prefix + "晚上好，今天晚餐你更想吃清淡点还是满足点？我按你的目标给你个选项。";
        }
        return prefix + "还没睡呀？要不要我帮你把今天的吃饭/喝水/运动做个轻量复盘，你只回我一句就行。";
    }

    private boolean shouldProactiveForUser(User user, LocalDateTime now) {
        if (user == null || user.getId() == null) return false;
        if (user.getStatus() != null && user.getStatus() == 0) return false;
        int days = Math.max(1, onlyLoginWithinDays);
        LocalDateTime cutoff = now.minusDays(days);
        LocalDateTime t = user.getLastLoginAt();
        if (t == null) t = user.getUpdatedAt();
        if (t == null) t = user.getCreatedAt();
        if (t != null && !t.isBefore(cutoff)) return true;

        try {
            String inboxSessionId = "inbox_" + user.getId();
            var lastUserMsg = conversationRecordRepository
                    .findTopByUserIdAndMessageTypeAndSessionIdNotOrderByTimestampDescIdDesc(
                            user.getId(),
                            ConversationRecord.MessageType.USER,
                            inboxSessionId
                    )
                    .orElse(null);
            if (lastUserMsg != null && lastUserMsg.getTimestamp() != null) {
                return !lastUserMsg.getTimestamp().isBefore(cutoff);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean shouldAttemptWindow(User user, LocalDateTime winStart) {
        if (user == null || user.getId() == null || winStart == null) return false;
        double prob = sendProbByTimeLabel(timeLabel(winStart));
        int threshold = (int) Math.round(Math.max(0.0, Math.min(1.0, prob)) * 1000.0);
        int h = stableHashMod(user.getId(), winStart, 1000);
        return h < threshold;
    }

    private double sendProbByTimeLabel(String label) {
        String t = String.valueOf(label == null ? "" : label).trim();
        if (t.equals("早上") || t.equals("上午")) return 0.30;
        if (t.equals("中午")) return 0.28;
        if (t.equals("下午")) return 0.24;
        if (t.equals("晚上")) return 0.22;
        return 0.12;
    }

    private boolean shouldIncludeContext(Long userId, LocalDateTime winStart) {
        if (userId == null || winStart == null) return false;
        double p = Math.max(0.0, Math.min(1.0, includeContextProb));
        int threshold = (int) Math.round(p * 1000.0);
        int h = stableHashMod(userId, "ctx@" + winStart, 1000);
        return h < threshold;
    }

    private int stableHashMod(Long userId, Object salt, int mod) {
        int m = Math.max(1, mod);
        int h = Objects.hash(String.valueOf(userId), String.valueOf(salt));
        return Math.floorMod(h, m);
    }

    private List<String> loadRecentUserQuotes(Long userId, LocalDateTime now) {
        if (userId == null) return List.of();
        int days = Math.max(1, lookbackDays);
        LocalDateTime start = (now == null ? LocalDateTime.now() : now).minusDays(days);
        LocalDateTime end = now == null ? LocalDateTime.now() : now;
        var page = conversationRecordRepository.findByUserIdAndMessageTypeAndTimestampBetweenOrderByTimestampDescIdDesc(
                userId,
                ConversationRecord.MessageType.USER,
                start,
                end,
                PageRequest.of(0, 120)
        );
        List<ConversationRecord> list = page == null ? List.of() : page.getContent();
        List<String> quotes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (list != null) {
            for (ConversationRecord r : list) {
                if (r == null) continue;
                String sid = String.valueOf(r.getSessionId() == null ? "" : r.getSessionId()).trim();
                if (sid.startsWith("inbox_")) continue;
                String text = String.valueOf(r.getContent() == null ? "" : r.getContent()).trim();
                if (text.isEmpty()) continue;
                String compact = text.replaceAll("\\s+", " ").trim();
                if (compact.length() <= 4) continue;
                String clipped = compact.length() > 36 ? compact.substring(0, 36) : compact;
                if (seen.add(clipped)) {
                    quotes.add(clipped);
                }
                if (quotes.size() >= 3) break;
            }
        }
        return quotes;
    }

    private boolean inQuietHours(LocalDateTime now) {
        int start = normalizeHour(quietStartHour);
        int end = normalizeHour(quietEndHour);
        int hour = now.getHour();
        if (start == end) return false;
        if (start < end) {
            return hour >= start && hour < end;
        }
        return hour >= start || hour < end;
    }

    private int normalizeHour(int hour) {
        int h = hour % 24;
        if (h < 0) h += 24;
        return h;
    }

    private String timeLabel(LocalDateTime now) {
        int h = now.getHour();
        if (h >= 5 && h <= 8) return "早上";
        if (h >= 9 && h <= 11) return "上午";
        if (h >= 12 && h <= 13) return "中午";
        if (h >= 14 && h <= 18) return "下午";
        if (h >= 19 && h <= 22) return "晚上";
        return "深夜";
    }

    private int windowMinutes() {
        return 30;
    }

    private LocalDateTime windowStart(LocalDateTime now) {
        LocalDateTime n = now == null ? LocalDateTime.now() : now;
        int step = windowMinutes();
        int minuteBucket = (n.getMinute() / step) * step;
        return n.truncatedTo(ChronoUnit.HOURS).plusMinutes(minuteBucket);
    }

    private String windowId(LocalDateTime winStart) {
        LocalDateTime ws = winStart == null ? windowStart(LocalDateTime.now()) : winStart;
        int idx = ws.getMinute() / windowMinutes();
        return ws.toLocalDate() + "_" + ws.getHour() + "_" + idx;
    }

    private String buildExplanationArgs(LocalDateTime now, LocalDateTime winStart, String text) {
        String dow = toCnWeekday(now.getDayOfWeek());
        String label = timeLabel(now);
        LocalDate date = now.toLocalDate();
        LocalTime wt = (winStart == null ? now : winStart).toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        String out = String.valueOf(text == null ? "" : text).trim();
        String clipped = out.length() > 32 ? out.substring(0, 32) : out;
        return "{\"date\":\"" + escape(date.toString()) + "\",\"time\":\"" + escape(now.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString()) + "\",\"weekday\":\"" + escape(dow) + "\",\"timeLabel\":\"" + escape(label) + "\",\"window\":\"" + escape(wt.toString()) + "\",\"preview\":\"" + escape(clipped) + "\"}";
    }

    private String toCnWeekday(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) return "";
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private String escape(String s) {
        String x = String.valueOf(s == null ? "" : s);
        return x.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
