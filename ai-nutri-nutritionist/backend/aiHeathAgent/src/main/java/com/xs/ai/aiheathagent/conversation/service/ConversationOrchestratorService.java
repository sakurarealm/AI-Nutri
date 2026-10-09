package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.ConversationInput;
import com.xs.ai.aiheathagent.conversation.model.DecisionActionType;
import com.xs.ai.aiheathagent.conversation.model.DecisionResult;
import com.xs.ai.aiheathagent.conversation.model.InputModality;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.model.UserEvent;
import com.xs.ai.aiheathagent.conversation.model.UserEventType;
import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.conversation.expose.SystemMessageType;
import com.xs.ai.aiheathagent.conversation.expose.SystemStateSnapshot;
import com.xs.ai.aiheathagent.entity.CapabilityKey;
import com.xs.ai.aiheathagent.entity.CapabilityState;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.ReminderIntentRecordRepository;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.vision.FoodCandidate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

@Service
public class ConversationOrchestratorService {
    private final EventDetectService eventDetectService;
    private final HealthDecisionService healthDecisionService;
    private final CompanionChatService companionChatService;
    private final ConversationHistoryQueryService historyQueryService;
    private final ProactiveTouchService proactiveTouchService;
    private final PlanGenerateService planGenerateService;
    private final SummaryReportService summaryReportService;
    private final ConversationRecordRepository conversationRecordRepository;
    private final SystemMessageMetaService systemMessageMetaService;
    private final UserRuntimeStateQueryService userRuntimeStateQueryService;
    private final UserStateQueryService userStateQueryService;
    private final ReminderIntentRecordRepository reminderIntentRecordRepository;
    private final ReminderScheduleService reminderScheduleService;

    public ConversationOrchestratorService(
            EventDetectService eventDetectService,
            HealthDecisionService healthDecisionService,
            CompanionChatService companionChatService,
            ConversationHistoryQueryService historyQueryService,
            ProactiveTouchService proactiveTouchService,
            PlanGenerateService planGenerateService,
            SummaryReportService summaryReportService,
            ConversationRecordRepository conversationRecordRepository,
            SystemMessageMetaService systemMessageMetaService,
            UserRuntimeStateQueryService userRuntimeStateQueryService,
            UserStateQueryService userStateQueryService,
            ReminderIntentRecordRepository reminderIntentRecordRepository,
            ReminderScheduleService reminderScheduleService
    ) {
        this.eventDetectService = eventDetectService;
        this.healthDecisionService = healthDecisionService;
        this.companionChatService = companionChatService;
        this.historyQueryService = historyQueryService;
        this.proactiveTouchService = proactiveTouchService;
        this.planGenerateService = planGenerateService;
        this.summaryReportService = summaryReportService;
        this.conversationRecordRepository = conversationRecordRepository;
        this.systemMessageMetaService = systemMessageMetaService;
        this.userRuntimeStateQueryService = userRuntimeStateQueryService;
        this.userStateQueryService = userStateQueryService;
        this.reminderIntentRecordRepository = reminderIntentRecordRepository;
        this.reminderScheduleService = reminderScheduleService;
    }

    public record OrchestratorResult(
            MessageContent reply,
            SystemMessageType systemMessageType,
            SystemStateSnapshot snapshot,
            UserEvent event,
            DecisionResult decision
    ) {
    }

    public OrchestratorResult handleUserInput(User user, ConversationInput input) {
        return handleUserInputInternal(user, input, null, true);
    }

    public OrchestratorResult handleUserInputStream(User user, ConversationInput input, Consumer<String> onDelta) {
        return handleUserInputInternal(user, input, onDelta, true);
    }

    public OrchestratorResult handleUserInputStream(User user, ConversationInput input, Consumer<String> onDelta, boolean buildSnapshot) {
        return handleUserInputInternal(user, input, onDelta, buildSnapshot);
    }

    public SystemStateSnapshot buildChatSnapshot(ConversationInput input, UserEvent recognizedEvent) {
        if (input == null || input.userId() == null) {
            return null;
        }
        try {
            return userRuntimeStateQueryService.buildForInput(input.userId(), input, recognizedEvent, null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private OrchestratorResult handleUserInputInternal(User user, ConversationInput input, Consumer<String> onDelta, boolean buildSnapshot) {
        LocalDateTime now = LocalDateTime.now();
        UserEvent event;
        try {
            event = eventDetectService.detectAndPersist(input);
        } catch (Exception ex) {
            String sessionId = input == null ? "" : input.sessionId();
            Long userId = input == null ? null : input.userId();
            var content = input == null ? MessageContent.text("") : input.content();
            event = new UserEvent(
                    null,
                    userId,
                    sessionId,
                    now,
                    com.xs.ai.aiheathagent.conversation.model.UserEventType.UNKNOWN,
                    java.util.Set.of(),
                    0.0,
                    false,
                    false,
                    content,
                    input == null ? com.xs.ai.aiheathagent.conversation.model.InputModality.TEXT : input.modality()
            );
        }

        DecisionResult decision;
        try {
            decision = healthDecisionService.decideRealtime(user, event);
        } catch (Exception ex) {
            decision = new DecisionResult(
                    user == null ? null : user.getId(),
                    "fallback:decision",
                    now.toString(),
                    now.plusMinutes(1),
                    EnumSet.of(DecisionActionType.NONE),
                    true
            );
        }

        if (decision.actions() != null && decision.actions().contains(DecisionActionType.GENERATE_DAILY_PLAN)) {
            try {
                planGenerateService.trigger(decision);
            } catch (Exception ignored) {
            }
        }
        if (decision.actions() != null && decision.actions().contains(DecisionActionType.GENERATE_SUMMARY)) {
            try {
                summaryReportService.trigger(decision);
            } catch (Exception ignored) {
            }
        }

        if (input != null && input.modality() == InputModality.IMAGE) {
            try {
                String sessionId = String.valueOf(input.sessionId() == null ? "" : input.sessionId()).trim();
                String text = "";
                try {
                    text = event == null || event.originalContent() == null ? "" : String.valueOf(event.originalContent().text() == null ? "" : event.originalContent().text()).trim();
                } catch (Exception ignored) {
                    text = "";
                }
                if (!sessionId.isEmpty()) {
                    ConversationRecord eventRec = new ConversationRecord();
                    eventRec.setSessionId(sessionId);
                    eventRec.setUserId(input.userId());
                    eventRec.setMessageType(ConversationRecord.MessageType.ASSISTANT);
                    eventRec.setContent(" ");
                    conversationRecordRepository.save(eventRec);
                    systemMessageMetaService.attach(input.userId(), eventRec.getId(), SystemMessageType.IMAGE_EVENT, null, null);

                    ConversationRecord rec = new ConversationRecord();
                    rec.setSessionId(sessionId);
                    rec.setUserId(input.userId());
                    rec.setMessageType(ConversationRecord.MessageType.ASSISTANT);
                    rec.setContent(text.isEmpty() ? " " : text);
                    conversationRecordRepository.save(rec);
                    systemMessageMetaService.attach(
                            input.userId(),
                            rec.getId(),
                            SystemMessageType.IMAGE_ANALYSIS,
                            null,
                            buildImageExplanationArgs(input.foodCandidates()),
                            null,
                            null,
                            null
                    );
                }
            } catch (Exception ignored) {
            }
            SystemStateSnapshot snapshot = buildSnapshot ? userRuntimeStateQueryService.buildForInput(
                    input.userId(),
                    input,
                    event,
                    null
            ) : null;
            return new OrchestratorResult(MessageContent.text(""), SystemMessageType.IMAGE_EVENT, snapshot, event, decision);
        }

        SystemStateSnapshot.IntentView intentView = null;
        IntentSignal summarySig = detectSummaryIntent(input == null ? null : input.content());
        if (summarySig != null) {
            Long userId = input.userId();
            String sessionId = input.sessionId();
            persistUserText(sessionId, userId, input.content());

            UserStateQueryService.CapabilityView cv = userStateQueryService.loadCapabilityView(userId);
            CapabilityState reqState = resolveRequiredState(cv, CapabilityKey.PROACTIVE_SUMMARY);
            boolean blocked = reqState != CapabilityState.ENABLED;
            intentView = new SystemStateSnapshot.IntentView(
                    "SUMMARY",
                    "",
                    "",
                    CapabilityKey.PROACTIVE_SUMMARY,
                    reqState,
                    blocked
            );

            SystemMessageType type = blocked ? SystemMessageType.CAPABILITY_GUIDE : SystemMessageType.SUMMARY;
            String replyText = "";
            if (!blocked) {
                User u = user;
                if (u == null && userId != null) {
                    u = new User();
                    u.setId(userId);
                }
                java.time.LocalDate today = java.time.LocalDate.now();
                try {
                    var out = summaryReportService.generateAndPersistSync(u, today, "on_demand");
                    replyText = out == null || out.userFacing() == null ? "" : String.valueOf(out.userFacing().text());
                } catch (Exception ignored) {
                    try {
                        var out = summaryReportService.generate(userId, today);
                        replyText = out == null || out.userFacing() == null ? "" : String.valueOf(out.userFacing().text());
                    } catch (Exception ignored2) {
                        replyText = "";
                    }
                }
                if (replyText == null || replyText.trim().isEmpty()) {
                    replyText = "今日复盘暂不可用，请稍后再试。";
                }
            }
            Long aiId = persistAssistantText(sessionId, userId, blocked ? " " : replyText);
            if (aiId != null) {
                systemMessageMetaService.attach(
                        userId,
                        aiId,
                        type,
                        blocked ? null : ExplanationCode.SUMMARY_ON_DEMAND,
                        blocked ? null : ("{\"date\":\"" + java.time.LocalDate.now() + "\",\"trigger\":\"on_demand\"}")
                );
            }
            SystemStateSnapshot snapshot = buildSnapshot ? userRuntimeStateQueryService.buildForInput(userId, input, event, intentView) : null;
            return new OrchestratorResult(MessageContent.text(blocked ? "" : replyText), type, snapshot, event, decision);
        }

        if (event != null && event.type() == UserEventType.MEAL && event.suggestWriteNutrition()) {
            Long userId = input.userId();
            String sessionId = input.sessionId();
            persistUserText(sessionId, userId, input.content());
            Long aiId = persistAssistantText(sessionId, userId, " ");
            if (aiId != null) {
                systemMessageMetaService.attach(userId, aiId, SystemMessageType.FACT_RECORD, null, null);
            }
            intentView = new SystemStateSnapshot.IntentView("FACT_RECORD", "", "", null, null, false);
            SystemStateSnapshot snapshot = buildSnapshot ? userRuntimeStateQueryService.buildForInput(userId, input, event, intentView) : null;
            return new OrchestratorResult(MessageContent.text(""), SystemMessageType.FACT_RECORD, snapshot, event, decision);
        }

        CancelSignal cancelSig = detectCancelReminderIntent(input == null ? null : input.content());
        if (cancelSig != null) {
            Long userId = input.userId();
            String sessionId = input.sessionId();
            persistUserText(sessionId, userId, input.content());
            int disabled = reminderScheduleService.disableMatching(userId, cancelSig.subject, cancelSig.scheduleTime, cancelSig.itemKeyPrefix);
            String replyText = disabled > 0
                    ? ("好的，我已为你关闭后续提醒：" + (cancelSig.subject == null || cancelSig.subject.isEmpty() ? "该事项" : cancelSig.subject) + "。")
                    : "我没有找到匹配的提醒计划。如果你告诉我药名或时间点，我可以帮你关闭。";
            Long aiId = persistAssistantText(sessionId, userId, replyText);
            if (aiId != null) {
                systemMessageMetaService.attach(userId, aiId, SystemMessageType.SYSTEM_ACK, ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN, null);
            }
            SystemStateSnapshot snapshot = buildSnapshot ? userRuntimeStateQueryService.buildForInput(userId, input, event, null) : null;
            return new OrchestratorResult(MessageContent.text(replyText), SystemMessageType.SYSTEM_ACK, snapshot, event, decision);
        }

        SkipSignal skipSig = detectSkipReminderIntent(input == null ? null : input.content());
        if (skipSig != null) {
            Long userId = input.userId();
            String sessionId = input.sessionId();
            persistUserText(sessionId, userId, input.content());
            boolean ok = false;
            try {
                String scheduleKey = buildScheduleKey(userId, skipSig.itemKeyPrefix, skipSig.subject, skipSig.scheduleTime);
                ok = reminderScheduleService.skipTodayByScheduleKey(userId, scheduleKey, LocalDate.now());
            } catch (Exception ignored) {
                ok = false;
            }
            String replyText = ok
                    ? ("好的，今天这次我就不提醒了：" + (skipSig.subject == null || skipSig.subject.isEmpty() ? "该事项" : skipSig.subject) + "。")
                    : "我没能定位到这条提醒计划。你告诉我药名和时间点，我就能帮你跳过今天。";
            Long aiId = persistAssistantText(sessionId, userId, replyText);
            if (aiId != null) {
                systemMessageMetaService.attach(userId, aiId, SystemMessageType.SYSTEM_ACK, ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN, null);
            }
            SystemStateSnapshot snapshot = buildSnapshot ? userRuntimeStateQueryService.buildForInput(userId, input, event, null) : null;
            return new OrchestratorResult(MessageContent.text(replyText), SystemMessageType.SYSTEM_ACK, snapshot, event, decision);
        }

        IntentSignal sig = detectReminderIntent(input == null ? null : input.content());
        if (sig != null) {
            UserStateQueryService.CapabilityView cv = userStateQueryService.loadCapabilityView(input.userId());
            boolean proactiveEnabled = cv != null && cv.proactiveReminderEnabled() == CapabilityState.ENABLED;
            CapabilityState reqState = resolveRequiredState(cv, sig.requiredCapability);
            boolean blocked = !proactiveEnabled || reqState != CapabilityState.ENABLED;
            intentView = new SystemStateSnapshot.IntentView(
                    sig.intentType,
                    sig.scheduleTime,
                    sig.subject,
                    sig.requiredCapability,
                    reqState,
                    blocked
            );
            String replyText = blocked
                    ? buildCapabilityGuideReply(sig, proactiveEnabled, reqState)
                    : buildPlanFeedbackReply(sig);
            SystemMessageType type = blocked ? SystemMessageType.CAPABILITY_GUIDE : SystemMessageType.PLAN_FEEDBACK;

            Long userId = input.userId();
            String sessionId = input.sessionId();
            persistUserText(sessionId, userId, input.content());
            Long aiId = persistAssistantText(sessionId, userId, replyText);
            if (aiId != null) {
                systemMessageMetaService.attach(userId, aiId, type, null, null);
            }
            if (!blocked) {
                persistReminderIntentFromChat(userId, sig, now);
            }
            SystemStateSnapshot snapshot = buildSnapshot ? userRuntimeStateQueryService.buildForInput(userId, input, event, intentView) : null;
            return new OrchestratorResult(MessageContent.text(replyText), type, snapshot, event, decision);
        }

        var history = historyQueryService.loadRecent(input.sessionId(), input.userId());
        var cmd = new CompanionChatService.CompanionChatCommand(
                input.sessionId(),
                input.userId(),
                history,
                input.content()
        );
        var chat = onDelta == null ? companionChatService.chat(cmd) : companionChatService.chatStream(cmd, onDelta);
        SystemStateSnapshot snapshot = buildSnapshot ? userRuntimeStateQueryService.buildForInput(input.userId(), input, event, null) : null;
        return new OrchestratorResult(chat.reply(), SystemMessageType.CHAT, snapshot, event, decision);
    }

    public boolean handleInboxPull(User user) {
        return false;
    }

    private static final Pattern TIME_HH_MM_PATTERN = Pattern.compile("(凌晨|早上|上午|中午|下午|晚上|夜里)?\\s*(\\d{1,2})\\s*[:：]\\s*(\\d{1,2})");
    private static final Pattern TIME_HOUR_PATTERN = Pattern.compile("(凌晨|早上|上午|中午|下午|晚上|夜里)?\\s*(\\d{1,2})\\s*点\\s*(半|一刻|三刻|\\d{1,2}(?:\\s*分)?)?");
    private static final Pattern TIME_CN_PATTERN = Pattern.compile("(凌晨|早上|上午|中午|下午|晚上|夜里)?\\s*([零〇一二两三四五六七八九十]{1,4})\\s*点\\s*(半|一刻|三刻|([零〇一二两三四五六七八九十]{1,4})(?:\\s*分)?)?");

    private IntentSignal detectReminderIntent(MessageContent content) {
        String t = content == null ? "" : String.valueOf(content.text() == null ? "" : content.text()).trim();
        if (t.isEmpty()) return null;
        String time = parseTimeFromText(t);
        if (time == null || time.isEmpty()) return null;
        boolean hasExplicitReminderWord = t.matches(".*(提醒|闹钟|定时|别忘|不要忘).*");
        boolean hasRecurringWord = t.matches(".*(每天|每日|每晚|每早|每周|每星期).*");
        boolean hasRecordWord = t.matches(".*(记一下|记下|记住|记下来|帮我记|帮我记下).*");
        if (!(hasExplicitReminderWord || hasRecurringWord || hasRecordWord)) return null;

        boolean supplement = t.matches(".*(鱼油|维生素|钙|锌|益生菌|DHA|dha|omega|辅酶|叶酸|铁剂).*");
        boolean medicine = t.matches(".*(吃药|用药|胰岛素|药|片|胶囊).*");
        String subject = supplement ? "保健品" : (medicine ? "用药" : "事项");
        if (t.contains("鱼油")) subject = "鱼油";
        if (t.contains("维生素")) subject = "维生素";
        CapabilityKey required = supplement ? CapabilityKey.SUPPLEMENT_REMINDER : CapabilityKey.MEDICATION_REMINDER;
        String intentType = supplement ? "SUPPLEMENT_REMINDER_REQUEST" : "MEDICATION_REMINDER_REQUEST";
        return new IntentSignal(intentType, time, subject, required);
    }

    private String parseTimeFromText(String raw) {
        String t = String.valueOf(raw == null ? "" : raw).trim();
        if (t.isEmpty()) return null;
        t = t.replace("：", ":");

        Matcher m1 = TIME_HH_MM_PATTERN.matcher(t);
        if (m1.find()) {
            String period = safe(m1.group(1));
            Integer h = parseIntSafe(m1.group(2));
            Integer mm = parseIntSafe(m1.group(3));
            return normalizeTime(period, h, mm);
        }

        Matcher m2 = TIME_HOUR_PATTERN.matcher(t);
        if (m2.find()) {
            String period = safe(m2.group(1));
            Integer h = parseIntSafe(m2.group(2));
            String minToken = safe(m2.group(3));
            Integer mm = parseMinuteToken(minToken);
            return normalizeTime(period, h, mm);
        }

        Matcher m3 = TIME_CN_PATTERN.matcher(t);
        if (m3.find()) {
            String period = safe(m3.group(1));
            Integer h = parseChineseNumber(safe(m3.group(2)));
            String minToken = safe(m3.group(3));
            Integer mm = null;
            if ("半".equals(minToken)) mm = 30;
            else if ("一刻".equals(minToken)) mm = 15;
            else if ("三刻".equals(minToken)) mm = 45;
            else if (!minToken.isEmpty()) mm = parseChineseNumber(minToken.replace("分", "").trim());
            return normalizeTime(period, h, mm);
        }

        return null;
    }

    private String normalizeTime(String period, Integer hour, Integer minute) {
        if (hour == null) return null;
        int h = hour;
        int m = minute == null ? 0 : minute;
        String p = String.valueOf(period == null ? "" : period).trim();
        if ((p.equals("下午") || p.equals("晚上") || p.equals("夜里") || p.equals("中午")) && h >= 0 && h <= 11) {
            if (!(p.equals("中午") && h == 0)) {
                h += 12;
            }
        }
        if (p.equals("凌晨") && h == 12) h = 0;
        if (h < 0 || h > 23) return null;
        if (m < 0 || m > 59) return null;
        return String.format("%02d:%02d", h, m);
    }

    private Integer parseMinuteToken(String token) {
        String t = String.valueOf(token == null ? "" : token).trim();
        if (t.isEmpty()) return null;
        if ("半".equals(t)) return 30;
        if ("一刻".equals(t)) return 15;
        if ("三刻".equals(t)) return 45;
        t = t.replace("分", "").trim();
        return parseIntSafe(t);
    }

    private Integer parseIntSafe(String s) {
        try {
            String t = String.valueOf(s == null ? "" : s).trim();
            if (t.isEmpty()) return null;
            return Integer.parseInt(t);
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(String s) {
        return String.valueOf(s == null ? "" : s).trim();
    }

    private Integer parseChineseNumber(String raw) {
        String t = String.valueOf(raw == null ? "" : raw).trim();
        if (t.isEmpty()) return null;
        t = t.replace("〇", "零");
        if (t.equals("零")) return 0;
        int total = 0;
        int current = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '十') {
                if (current == 0) current = 1;
                total += current * 10;
                current = 0;
                continue;
            }
            int v = switch (c) {
                case '零' -> 0;
                case '一' -> 1;
                case '二' -> 2;
                case '两' -> 2;
                case '三' -> 3;
                case '四' -> 4;
                case '五' -> 5;
                case '六' -> 6;
                case '七' -> 7;
                case '八' -> 8;
                case '九' -> 9;
                default -> -1;
            };
            if (v < 0) return null;
            current = current * 10 + v;
        }
        total += current;
        return total;
    }

    private CapabilityState resolveRequiredState(UserStateQueryService.CapabilityView cv, CapabilityKey k) {
        if (cv == null || k == null) return CapabilityState.OFF;
        return switch (k) {
            case MEDICATION_REMINDER -> cv.medicationReminderEnabled();
            case SUPPLEMENT_REMINDER -> cv.supplementReminderEnabled();
            case EXERCISE_SUGGESTION -> cv.exerciseSuggestionEnabled();
            case PROACTIVE_SUMMARY -> cv.proactiveSummaryEnabled();
            case PROACTIVE_REMINDER -> cv.proactiveReminderEnabled();
        };
    }

    private String buildCapabilityGuideReply(IntentSignal sig, boolean proactiveEnabled, CapabilityState reqState) {
        String subject = sig == null ? "事项" : sig.subject;
        String time = sig == null ? "" : sig.scheduleTime;
        String head = "已记住：每天 " + time + " " + subject + "。";
        String gate = proactiveEnabled ? "" : "提醒功能当前未开启，因此我不会主动提醒。";
        String sub = (reqState == CapabilityState.ENABLED) ? "" : "该类提醒还未开启。";
        String tip = "你可以在设置里开启提醒，或先用手机闹钟辅助。";
        return head + "\n" + (gate.isEmpty() ? sub : gate) + (sub.isEmpty() || !gate.isEmpty() ? "" : "\n" + sub) + "\n" + tip;
    }

    private String buildPlanFeedbackReply(IntentSignal sig) {
        String subject = sig == null ? "事项" : sig.subject;
        String time = sig == null ? "" : sig.scheduleTime;
        return "已记住：每天 " + time + " " + subject + "。我会把它作为你的健康记录的一部分，并在你允许的前提下提供提醒与复盘。";
    }

    private void persistReminderIntentFromChat(Long userId, IntentSignal sig, LocalDateTime now) {
        try {
            if (userId == null || sig == null) return;
            String scheduleTime = reminderScheduleService.normalizeTimeSlot(sig.scheduleTime);
            if (scheduleTime.isEmpty()) return;
            String subject = String.valueOf(sig.subject == null ? "" : sig.subject).trim();
            String prefix = sig.requiredCapability == CapabilityKey.SUPPLEMENT_REMINDER ? "supplement_" : "medication_";
            String itemKey = prefix + (subject.isEmpty() ? "事项" : subject);
            String scheduleKey = "reminder:" + userId + ":" + itemKey + ":" + scheduleTime;
            LocalDate date = LocalDate.now();
            String decisionKey = scheduleKey + ":" + date;
            if (reminderIntentRecordRepository.existsByUserIdAndDecisionKey(userId, decisionKey)) return;

            try {
                reminderScheduleService.upsertSchedule(userId, scheduleKey, scheduleTime, itemKey, subject);
            } catch (Exception ignored) {
            }

            LocalDateTime at = now == null ? LocalDateTime.now() : now;
            ReminderIntentRecord rec = new ReminderIntentRecord();
            rec.setUserId(userId);
            rec.setPlanDate(date);
            rec.setTimeSlot(scheduleTime);
            rec.setItemKey(itemKey);
            rec.setDecisionKey(decisionKey);
            rec.setWindowId(date + ":" + scheduleTime);
            rec.setValidUntil(date.atTime(java.time.LocalTime.parse(scheduleTime)).plusHours(8));
            rec.setChannel("INBOX");
            rec.setStatus("PENDING");
            rec.setExplanationCode(ExplanationCode.REMINDER_INTENT_FROM_DAILY_PLAN);
            rec.setExplanationArgs("{\"source\":\"chat\",\"scheduleKey\":\"" + escapeJson(scheduleKey) + "\",\"planDate\":\"" + escapeJson(date.toString()) + "\",\"timeSlot\":\"" + escapeJson(scheduleTime) + "\",\"scheduleTime\":\"" + escapeJson(scheduleTime) + "\",\"itemKey\":\"" + escapeJson(itemKey) + "\",\"subject\":\"" + escapeJson(subject) + "\"}");
            reminderIntentRecordRepository.save(rec);
        } catch (Exception ignored) {
        }
    }

    private String escapeJson(String s) {
        String v = String.valueOf(s == null ? "" : s);
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void persistUserText(String sessionId, Long userId, MessageContent content) {
        String t = content == null ? "" : String.valueOf(content.text() == null ? "" : content.text());
        ConversationRecord rec = new ConversationRecord();
        rec.setSessionId(sessionId);
        rec.setUserId(userId);
        rec.setMessageType(ConversationRecord.MessageType.USER);
        rec.setContent(t);
        conversationRecordRepository.save(rec);
    }

    private Long persistAssistantText(String sessionId, Long userId, String text) {
        String t = String.valueOf(text == null ? "" : text);
        ConversationRecord rec = new ConversationRecord();
        rec.setSessionId(sessionId);
        rec.setUserId(userId);
        rec.setMessageType(ConversationRecord.MessageType.ASSISTANT);
        rec.setContent(t);
        conversationRecordRepository.save(rec);
        return rec.getId();
    }

    private record IntentSignal(
            String intentType,
            String scheduleTime,
            String subject,
            CapabilityKey requiredCapability
    ) {
    }

    private IntentSignal detectSummaryIntent(MessageContent content) {
        String t = String.valueOf(content == null || content.text() == null ? "" : content.text()).trim();
        if (t.isEmpty()) return null;
        if (t.matches(".*(总结|复盘|汇总|今天怎么样|今天如何).*")) {
            return new IntentSignal("SUMMARY", "", "", CapabilityKey.PROACTIVE_SUMMARY);
        }
        return null;
    }

    private CancelSignal detectCancelReminderIntent(MessageContent content) {
        String t = content == null ? "" : String.valueOf(content.text() == null ? "" : content.text()).trim();
        if (t.isEmpty()) return null;
        if (!t.matches(".*(不用了|不需要|停止|取消|别提醒|不要提醒).*")) return null;
        String time = parseTimeFromText(t);

        boolean supplement = t.matches(".*(鱼油|维生素|钙|锌|益生菌|DHA|dha|omega|辅酶|叶酸|铁剂).*");
        boolean medicine = t.matches(".*(吃药|用药|胰岛素|降压药|药|片|胶囊).*");
        String subject = extractSubject(t, supplement, medicine);
        String prefix = supplement ? "supplement_" : (medicine ? "medication_" : "");
        return new CancelSignal(subject, time, prefix);
    }

    private SkipSignal detectSkipReminderIntent(MessageContent content) {
        String t = content == null ? "" : String.valueOf(content.text() == null ? "" : content.text()).trim();
        if (t.isEmpty()) return null;
        if (!t.matches(".*(今天|这次).*") || !t.matches(".*(不吃|不服|不需要|跳过|不提醒).*")) return null;
        String time = parseTimeFromText(t);
        boolean supplement = t.matches(".*(鱼油|维生素|钙|锌|益生菌|DHA|dha|omega|辅酶|叶酸|铁剂).*");
        boolean medicine = t.matches(".*(吃药|用药|胰岛素|降压药|药|片|胶囊).*");
        String subject = extractSubject(t, supplement, medicine);
        String prefix = supplement ? "supplement_" : (medicine ? "medication_" : "");
        if (prefix.isEmpty()) return null;
        if (time == null || time.isEmpty()) return null;
        return new SkipSignal(subject, time, prefix);
    }

    private String extractSubject(String text, boolean supplement, boolean medicine) {
        String t = String.valueOf(text == null ? "" : text);
        if (t.contains("鱼油")) return "鱼油";
        if (t.contains("维生素")) return "维生素";
        if (t.contains("降压药")) return "降压药";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:吃|服用|用)\\s*([\\p{IsHan}A-Za-z0-9]{1,12})").matcher(t);
        if (m.find()) {
            String s = String.valueOf(m.group(1) == null ? "" : m.group(1)).trim();
            if (!s.isEmpty() && !s.equals("药")) return s;
        }
        if (supplement) return "保健品";
        if (medicine) return "用药";
        return "";
    }

    private String buildScheduleKey(Long userId, String prefix, String subject, String scheduleTime) {
        String uid = String.valueOf(userId == null ? "" : userId);
        String p = String.valueOf(prefix == null ? "" : prefix).trim();
        String sub = String.valueOf(subject == null ? "" : subject).trim();
        String time = String.valueOf(scheduleTime == null ? "" : scheduleTime).trim();
        String itemKey = p + (sub.isEmpty() ? "事项" : sub);
        return "reminder:" + uid + ":" + itemKey + ":" + time;
    }

    private record CancelSignal(String subject, String scheduleTime, String itemKeyPrefix) {}
    private record SkipSignal(String subject, String scheduleTime, String itemKeyPrefix) {}

    private String buildImageExplanationArgs(List<FoodCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return "{\"items\":[]}";
        StringBuilder sb = new StringBuilder();
        sb.append("{\"items\":[");
        int n = 0;
        for (int i = 0; i < Math.min(6, candidates.size()); i++) {
            FoodCandidate c = candidates.get(i);
            if (c == null) continue;
            String name = String.valueOf(c.getName() == null ? "" : c.getName()).trim();
            if (name.isEmpty()) continue;
            if (n > 0) sb.append(",");
            sb.append("{\"name\":\"").append(escapeJson(name)).append("\"");
            sb.append(",\"probability\":").append(c.getProbability());
            sb.append(",\"calorie\":").append(c.getCalorie());
            sb.append("}");
            n++;
        }
        sb.append("]}");
        return sb.toString();
    }
}
