package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.client.DeepSeekChatClient;
import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import com.xs.ai.aiheathagent.conversation.model.ConversationRole;
import com.xs.ai.aiheathagent.conversation.model.DailySummary;
import com.xs.ai.aiheathagent.conversation.model.DecisionActionType;
import com.xs.ai.aiheathagent.conversation.model.DecisionResult;
import com.xs.ai.aiheathagent.conversation.model.ClinicalSummary;
import com.xs.ai.aiheathagent.conversation.model.ClinicalDatum;
import com.xs.ai.aiheathagent.conversation.model.MessageContent;
import com.xs.ai.aiheathagent.conversation.model.MessageChannel;
import com.xs.ai.aiheathagent.conversation.prompt.Prompt;
import com.xs.ai.aiheathagent.conversation.prompt.PromptProvider;
import com.xs.ai.aiheathagent.conversation.prompt.PromptRole;
import com.xs.ai.aiheathagent.dto.resp.NutritionDayResp;
import com.xs.ai.aiheathagent.dto.resp.NutritionFoodItemResp;
import com.xs.ai.aiheathagent.dto.resp.NutritionMemberResp;
import com.xs.ai.aiheathagent.dto.resp.OrganImpactResp;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.entity.DailyPlanRecord;
import com.xs.ai.aiheathagent.entity.DailySummaryRecord;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.DailyPlanRecordRepository;
import com.xs.ai.aiheathagent.repository.DailySummaryRecordRepository;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import com.xs.ai.aiheathagent.service.NutritionService;
import com.xs.ai.aiheathagent.service.OrganImpactService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class SummaryReportService {
    private final UserRepo userRepo;
    private final UserEventRecordRepository userEventRecordRepository;
    private final DailyPlanRecordRepository dailyPlanRecordRepository;
    private final DailySummaryRecordRepository dailySummaryRecordRepository;
    private final HealthDecisionService healthDecisionService;
    private final ProactiveTouchService proactiveTouchService;
    private final DeepSeekChatClient deepSeekChatClient;
    private final PromptProvider promptProvider;
    private final NutritionService nutritionService;
    private final OrganImpactService organImpactService;
    private final ConversationRecordRepository conversationRecordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Value("${summary.catchup.enabled:true}")
    private boolean catchupEnabled;

    @Value("${summary.catchup.after-hour:22}")
    private int catchupAfterHour;

    @Value("${summary.catchup.after-minute:10}")
    private int catchupAfterMinute;

    @Value("${summary.scan-page-size:300}")
    private int scanPageSize;

    public SummaryReportService(
            UserRepo userRepo,
            UserEventRecordRepository userEventRecordRepository,
            DailyPlanRecordRepository dailyPlanRecordRepository,
            DailySummaryRecordRepository dailySummaryRecordRepository,
            HealthDecisionService healthDecisionService,
            ProactiveTouchService proactiveTouchService,
            DeepSeekChatClient deepSeekChatClient,
            PromptProvider promptProvider,
            NutritionService nutritionService,
            OrganImpactService organImpactService,
            ConversationRecordRepository conversationRecordRepository
    ) {
        this.userRepo = userRepo;
        this.userEventRecordRepository = userEventRecordRepository;
        this.dailyPlanRecordRepository = dailyPlanRecordRepository;
        this.dailySummaryRecordRepository = dailySummaryRecordRepository;
        this.healthDecisionService = healthDecisionService;
        this.proactiveTouchService = proactiveTouchService;
        this.deepSeekChatClient = deepSeekChatClient;
        this.promptProvider = promptProvider;
        this.nutritionService = nutritionService;
        this.organImpactService = organImpactService;
        this.conversationRecordRepository = conversationRecordRepository;
    }

    @Async
    public void trigger(DecisionResult decision) {
        if (decision == null || decision.userId() == null) {
            return;
        }
        generate(decision.userId(), LocalDate.now());
    }

    @Scheduled(cron = "${summary.daily.cron:0 10 22 * * *}")
    public void scheduledDailySummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        Set<Long> done = new HashSet<>();
        try {
            done.addAll(dailySummaryRecordRepository.listUserIdsByDateAndExplanationCode(date, ExplanationCode.SUMMARY_SCHEDULED_DAILY));
        } catch (Exception ignored) {
        }
        int page = 0;
        int size = Math.max(20, scanPageSize);
        while (true) {
            Page<User> users;
            try {
                users = userRepo.findActiveUsers(PageRequest.of(page, size));
            } catch (Exception e) {
                users = Page.empty();
            }
            if (users == null || users.isEmpty()) break;
            for (User u : users.getContent()) {
                try {
                    if (u == null || u.getId() == null) continue;
                    if (done.contains(u.getId())) continue;
                    DecisionResult d = healthDecisionService.decideDailySummaryGeneration(u, date, now);
                    if (d.actions() != null && d.actions().contains(DecisionActionType.GENERATE_SUMMARY)) {
                        generateAndPersist(u, date);
                    }
                } catch (Exception ignored) {
                }
            }
            if (!users.hasNext()) break;
            page++;
        }
    }

    @Scheduled(fixedDelayString = "${summary.catchup.interval-ms:600000}")
    public void catchupDailySummary() {
        if (!catchupEnabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        LocalDateTime after = date.atTime(Math.max(0, Math.min(23, catchupAfterHour)), Math.max(0, Math.min(59, catchupAfterMinute)));
        if (now.isBefore(after)) {
            return;
        }
        Set<Long> done = new HashSet<>();
        try {
            done.addAll(dailySummaryRecordRepository.listUserIdsByDateAndExplanationCode(date, ExplanationCode.SUMMARY_SCHEDULED_DAILY));
        } catch (Exception ignored) {
        }
        int page = 0;
        int size = Math.max(20, scanPageSize);
        while (true) {
            Page<User> users;
            try {
                users = userRepo.findActiveUsers(PageRequest.of(page, size));
            } catch (Exception e) {
                users = Page.empty();
            }
            if (users == null || users.isEmpty()) break;
            for (User u : users.getContent()) {
                try {
                    if (u == null || u.getId() == null) continue;
                    if (done.contains(u.getId())) continue;
                    DecisionResult d = healthDecisionService.decideDailySummaryGeneration(u, date, now);
                    if (d.actions() != null && d.actions().contains(DecisionActionType.GENERATE_SUMMARY)) {
                        generateAndPersist(u, date);
                    }
                } catch (Exception ignored) {
                }
            }
            if (!users.hasNext()) break;
            page++;
        }
    }

    @Async
    public void generateAndPersist(User user, LocalDate date) {
        generateAndPersistSync(user, date);
    }

    public DailySummary generateAndPersistSync(User user, LocalDate date) {
        return generateAndPersistSync(user, date, "scheduled");
    }

    public DailySummary generateAndPersistSync(User user, LocalDate date, String trigger) {
        if (user == null || user.getId() == null || date == null) return null;
        DailySummary out = generate(user.getId(), date);
        String clinicalJson = "";
        try {
            var sb = new StringBuilder();
            sb.append("{\"schema\":\"").append(out.clinicalSummary() == null ? "v1" : out.clinicalSummary().schemaVersion()).append("\",\"data\":[");
            List<ClinicalDatum> data = out.clinicalSummary() == null ? List.of() : out.clinicalSummary().data();
            for (int i = 0; i < data.size(); i++) {
                ClinicalDatum d = data.get(i);
                if (d == null) continue;
                if (i > 0) sb.append(",");
                sb.append("{\"key\":\"").append(escape(d.key())).append("\",\"value\":\"").append(escape(d.value())).append("\",\"unit\":\"").append(escape(d.unit())).append("\"}");
            }
            sb.append("]}");
            clinicalJson = sb.toString();
        } catch (Exception ignored) {
            clinicalJson = "{}";
        }

        DailySummaryRecord rec = new DailySummaryRecord();
        rec.setUserId(user.getId());
        rec.setDate(date);
        rec.setCreatedAt(LocalDateTime.now());
        rec.setUserText(out.userFacing() == null ? "" : String.valueOf(out.userFacing().text()));
        rec.setFamilyText(out.familyFacing() == null ? "" : String.valueOf(out.familyFacing().text()));
        rec.setClinicalJson(clinicalJson);
        boolean scheduled = isScheduledTrigger(trigger);
        rec.setExplanationCode(scheduled ? ExplanationCode.SUMMARY_SCHEDULED_DAILY : ExplanationCode.SUMMARY_ON_DEMAND);
        String trig = scheduled ? "scheduled" : "on_demand";
        rec.setExplanationArgs("{\"date\":\"" + escape(date.toString()) + "\",\"trigger\":\"" + escape(trig) + "\"}");
        rec = dailySummaryRecordRepository.save(rec);

        if (scheduled) {
            try {
                String decisionKey = "summary:" + user.getId() + ":" + date + ":" + rec.getId();
                DecisionResult d = new DecisionResult(
                        user.getId(),
                        decisionKey,
                        "summary:" + date,
                        LocalDateTime.now().plusHours(6),
                        EnumSet.of(DecisionActionType.PROACTIVE_INBOX),
                        false
                );
                proactiveTouchService.execute(user, d, MessageContent.text(String.valueOf(out.userFacing() == null ? "" : out.userFacing().text())), MessageChannel.INBOX, rec.getExplanationCode(), rec.getExplanationArgs());
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public DailySummary generate(Long userId, LocalDate date) {
        if (userId == null || date == null) {
            return new DailySummary(0L, LocalDate.now(), MessageContent.text(""), MessageContent.text(""), new ClinicalSummary("v1", List.of()));
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<UserEventRecord> events = userEventRecordRepository.findByUserIdAndOccurredAtBetweenOrderByOccurredAtAsc(userId, start, end);

        int meal = 0;
        int medicine = 0;
        for (UserEventRecord e : events) {
            if (e == null) continue;
            String t = String.valueOf(e.getEventType() == null ? "" : e.getEventType());
            if ("MEAL".equals(t)) meal++;
            if ("MEDICINE".equals(t)) medicine++;
        }

        boolean hasPlan = dailyPlanRecordRepository.existsByUserIdAndPlanDate(userId, date);
        String planJson = hasPlan ? loadPlanJson(userId, date) : "";

        Long memberId = resolveDefaultMemberId(userId);
        NutritionDayResp day = null;
        OrganImpactResp impact = null;
        if (memberId != null) {
            try {
                day = nutritionService.getDay(userId, memberId, date.toString());
            } catch (Exception ignored) {
                day = null;
            }
            try {
                impact = organImpactService.getOrganImpact(userId, memberId, date.toString());
            } catch (Exception ignored) {
                impact = null;
            }
        }

        ChatStats chatStats = buildChatStats(userId, start, end);

        String userText = buildUserFacingSummary(userId, date, hasPlan, planJson, meal, medicine, day, impact, chatStats);
        String familyText = buildFamilyFacingSummary(date, meal, medicine, day);

        List<ClinicalDatum> data = new ArrayList<>();
        data.add(new ClinicalDatum("meal_event_count", String.valueOf(meal), "count"));
        data.add(new ClinicalDatum("medicine_event_count", String.valueOf(medicine), "count"));
        data.add(new ClinicalDatum("daily_plan_exists", hasPlan ? "1" : "0", "bool"));
        if (day != null) {
            data.add(new ClinicalDatum("calorie_consumed", String.valueOf(Math.max(0, day.getCalorieConsumed())), "kcal"));
            data.add(new ClinicalDatum("calorie_target", String.valueOf(Math.max(0, day.getCalorieTarget())), "kcal"));
            Map<String, Double> totals = day.getMacroTotals();
            if (totals != null) {
                data.add(new ClinicalDatum("macro_carb_g", String.valueOf(Math.max(0.0, safeDouble(totals, "carb"))), "g"));
                data.add(new ClinicalDatum("macro_protein_g", String.valueOf(Math.max(0.0, safeDouble(totals, "protein"))), "g"));
                data.add(new ClinicalDatum("macro_fat_g", String.valueOf(Math.max(0.0, safeDouble(totals, "fat"))), "g"));
            }
        }

        return new DailySummary(
                userId,
                date,
                MessageContent.text(userText),
                MessageContent.text(familyText),
                new ClinicalSummary("v1", data)
        );
    }

    private record ChatStats(
            long userMessages,
            long aiMessages,
            String firstUserAt,
            String lastUserAt,
            List<String> userQuotes
    ) {
    }

    private ChatStats buildChatStats(Long userId, LocalDateTime start, LocalDateTime end) {
        if (userId == null) {
            return new ChatStats(0, 0, "", "", List.of());
        }
        var page = conversationRecordRepository.findByUserIdAndTimestampBetweenOrderByTimestampDescIdDesc(
                userId,
                start,
                end,
                PageRequest.of(0, 240)
        );
        List<ConversationRecord> list = page == null ? List.of() : page.getContent();
        long userCount = 0;
        long aiCount = 0;
        LocalDateTime first = null;
        LocalDateTime last = null;
        List<String> quotes = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ConversationRecord r = list.get(i);
                if (r == null) continue;
                String sid = String.valueOf(r.getSessionId() == null ? "" : r.getSessionId()).trim();
                if (sid.startsWith("inbox_")) continue;
                String text = String.valueOf(r.getContent() == null ? "" : r.getContent()).trim();
                if (text.isEmpty()) continue;

                boolean isUser = r.getMessageType() != null && r.getMessageType().name().equalsIgnoreCase("USER");
                if (isUser) userCount++;
                else aiCount++;

                if (isUser) {
                    LocalDateTime ts = r.getTimestamp();
                    if (ts != null) {
                        if (last == null || ts.isAfter(last)) last = ts;
                        if (first == null || ts.isBefore(first)) first = ts;
                    }
                    String compact = text.replaceAll("\\s+", " ").trim();
                    if (!compact.isEmpty() && compact.length() > 4) {
                        String clipped = compact.length() > 36 ? compact.substring(0, 36) : compact;
                        if (!seen.contains(clipped)) {
                            seen.add(clipped);
                            quotes.add(clipped);
                        }
                    }
                }
                if (quotes.size() >= 4) break;
            }
        }
        String firstAt = first == null ? "" : formatHHmm(first.toLocalTime());
        String lastAt = last == null ? "" : formatHHmm(last.toLocalTime());
        return new ChatStats(userCount, aiCount, firstAt, lastAt, quotes);
    }

    private String buildUserFacingSummary(Long userId, LocalDate date, boolean hasPlan, String planJson, int mealCount, int medicineCount, NutritionDayResp day, OrganImpactResp impact, ChatStats chatStats) {
        String fallback = buildFallbackUserText(date, hasPlan, mealCount, medicineCount, day, impact, chatStats);
        if (deepSeekChatClient == null || !deepSeekChatClient.isConfigured() || promptProvider == null) {
            return fallback;
        }
        try {
            Prompt p = promptProvider.get(PromptRole.SUMMARY_USER);
            String system = p == null ? "" : String.valueOf(p.systemText() == null ? "" : p.systemText()).trim();
            String userJson = buildSummaryUserJson(userId, date, hasPlan, planJson, mealCount, medicineCount, day, impact, chatStats);
            List<ConversationMessage> messages = new ArrayList<>();
            if (!system.isEmpty()) {
                messages.add(new ConversationMessage(ConversationRole.SYSTEM, MessageContent.text(system), LocalDateTime.now()));
            }
            messages.add(new ConversationMessage(ConversationRole.USER, MessageContent.text(userJson), LocalDateTime.now()));
            String completion = deepSeekChatClient.complete(messages);
            String out = String.valueOf(completion == null ? "" : completion).trim();
            if (out.isEmpty()) return fallback;
            if (out.length() > 2200) {
                out = out.substring(0, 2200);
            }
            return out;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String buildFamilyFacingSummary(LocalDate date, int mealCount, int medicineCount, NutritionDayResp day) {
        String d = date == null ? "" : date.toString();
        String base = d.isEmpty() ? "今日关怀摘要：" : (d + " 关怀摘要：");
        String meal = day == null ? "" : summarizeMealsOneLine(day);
        String counts = "记录饮食 " + Math.max(0, mealCount) + " 次" + (medicineCount > 0 ? ("，用药/补剂 " + medicineCount + " 次") : "");
        if (!meal.isEmpty()) {
            return base + counts + "；三餐概况：" + meal;
        }
        return base + counts + "。";
    }

    private String buildFallbackUserText(LocalDate date, boolean hasPlan, int mealCount, int medicineCount, NutritionDayResp day, OrganImpactResp impact, ChatStats chatStats) {
        String d = date == null ? "" : date.toString();
        StringBuilder sb = new StringBuilder();
        sb.append("今日复盘（").append(d.isEmpty() ? "今天" : d).append("）：");
        sb.append("饮食记录 ").append(Math.max(0, mealCount)).append(" 次");
        if (medicineCount > 0) sb.append("，用药/补剂 ").append(Math.max(0, medicineCount)).append(" 次");
        sb.append(hasPlan ? "；计划已生成。" : "；计划暂无。");

        if (day != null) {
            sb.append("\n[三餐记录] ").append(summarizeMealsOneLine(day));
            sb.append("\n[营养与目标] ");
            sb.append("热量 ").append(Math.max(0, day.getCalorieConsumed())).append("/").append(Math.max(0, day.getCalorieTarget())).append(" 千卡");
            Map<String, Integer> pct = day.getMacroPercent();
            if (pct != null) {
                sb.append("；碳水 ").append(Math.max(0, safeInt(pct, "carb"))).append("%");
                sb.append(" 蛋白 ").append(Math.max(0, safeInt(pct, "protein"))).append("%");
                sb.append(" 脂肪 ").append(Math.max(0, safeInt(pct, "fat"))).append("%");
            }
        } else {
            sb.append("\n[三餐记录] 暂无营养册记录");
            sb.append("\n[营养与目标] 暂无数据");
        }

        sb.append("\n[人体影响图] ");
        String impactLine = summarizeImpactOneLine(impact);
        sb.append(impactLine.isEmpty() ? "暂无数据" : impactLine);

        sb.append("\n[计划执行] ").append(hasPlan ? "计划在，可继续按计划推进；晚间可做日终回顾。" : "建议先生成计划，明天更好执行。");

        sb.append("\n[聊天状态] ");
        long u = chatStats == null ? 0 : Math.max(0, chatStats.userMessages());
        long a = chatStats == null ? 0 : Math.max(0, chatStats.aiMessages());
        sb.append("今天我们聊了 ").append(u).append(" 句（我回复 ").append(a).append(" 句）");
        if (chatStats != null && !chatStats.firstUserAt().isEmpty() && !chatStats.lastUserAt().isEmpty()) {
            sb.append("，主要在 ").append(chatStats.firstUserAt()).append("–").append(chatStats.lastUserAt()).append("。");
        } else {
            sb.append("。");
        }
        if (chatStats != null && chatStats.userQuotes() != null && !chatStats.userQuotes().isEmpty()) {
            sb.append("你今天提到：").append("“").append(chatStats.userQuotes().get(0)).append("…”");
        }

        sb.append("\n[明日一条] 下一餐：主食减 1/4，蔬菜加 1 拳。");
        return sb.toString();
    }

    private String buildSummaryUserJson(Long userId, LocalDate date, boolean hasPlan, String planJson, int mealCount, int medicineCount, NutritionDayResp day, OrganImpactResp impact, ChatStats chatStats) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"date\":\"").append(escape(date == null ? "" : date.toString())).append("\",");
        sb.append("\"userId\":\"").append(escape(String.valueOf(userId == null ? "" : userId))).append("\",");
        sb.append("\"plan\":{");
        sb.append("\"exists\":").append(hasPlan ? "true" : "false").append(",");
        String planDataJson = String.valueOf(planJson == null ? "" : planJson).trim();
        sb.append("\"data\":").append(planDataJson.isEmpty() ? "null" : planDataJson);
        sb.append("},");
        sb.append("\"events\":{");
        sb.append("\"mealCount\":").append(Math.max(0, mealCount)).append(",");
        sb.append("\"medicineCount\":").append(Math.max(0, medicineCount));
        sb.append("},");
        sb.append("\"nutrition\":");
        if (day == null) {
            sb.append("null,");
        } else {
            sb.append("{");
            sb.append("\"calorieConsumed\":").append(Math.max(0, day.getCalorieConsumed())).append(",");
            sb.append("\"calorieTarget\":").append(Math.max(0, day.getCalorieTarget())).append(",");
            sb.append("\"macroPercent\":").append(toJsonIntMap(day.getMacroPercent())).append(",");
            sb.append("\"macroTotals\":").append(toJsonDoubleMap(day.getMacroTotals())).append(",");
            sb.append("\"breakfast\":").append(toJsonFoods(day.getBreakfastFoods())).append(",");
            sb.append("\"lunch\":").append(toJsonFoods(day.getLunchFoods())).append(",");
            sb.append("\"dinner\":").append(toJsonFoods(day.getDinnerFoods())).append(",");
            sb.append("\"snacks\":").append(toJsonSnackFoods(day));
            sb.append("},");
        }
        sb.append("\"organImpact\":");
        if (impact == null) {
            sb.append("null,");
        } else {
            sb.append("{");
            sb.append("\"profile\":{");
            var profile = impact.getProfile();
            sb.append("\"foodsCount\":").append(profile == null ? 0 : Math.max(0, profile.getFoodsCount())).append(",");
            sb.append("\"calorieConsumed\":").append(profile == null ? 0 : Math.max(0, profile.getCalorieConsumed())).append(",");
            sb.append("\"calorieTarget\":").append(profile == null ? 0 : Math.max(0, profile.getCalorieTarget()));
            sb.append("},");
            sb.append("\"impacts\":").append(toJsonOrganImpacts(impact));
            sb.append("},");
        }
        sb.append("\"chat\":");
        if (chatStats == null) {
            sb.append("null");
        } else {
            sb.append("{");
            sb.append("\"userMessages\":").append(Math.max(0, chatStats.userMessages())).append(",");
            sb.append("\"aiMessages\":").append(Math.max(0, chatStats.aiMessages())).append(",");
            sb.append("\"firstUserAt\":\"").append(escape(chatStats.firstUserAt())).append("\",");
            sb.append("\"lastUserAt\":\"").append(escape(chatStats.lastUserAt())).append("\",");
            sb.append("\"userQuotes\":").append(toJsonStringList(chatStats.userQuotes()));
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private String loadPlanJson(Long userId, LocalDate planDate) {
        if (userId == null || planDate == null) return "";
        try {
            DailyPlanRecord rec = dailyPlanRecordRepository.findTopByUserIdAndPlanDateOrderByIdDesc(userId, planDate).orElse(null);
            if (rec == null) return "";
            String raw = String.valueOf(rec.getPlanJson() == null ? "" : rec.getPlanJson()).trim();
            if (raw.isEmpty()) return "";
            return objectMapper.readTree(raw).toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String summarizeMealsOneLine(NutritionDayResp day) {
        if (day == null) return "";
        String b = joinFoodNames(day.getBreakfastFoods(), 4);
        String l = joinFoodNames(day.getLunchFoods(), 4);
        String d = joinFoodNames(day.getDinnerFoods(), 4);
        String s = joinSnackFoodNames(day, 4);
        StringBuilder sb = new StringBuilder();
        sb.append("早餐 ").append(b.isEmpty() ? "未记录" : b);
        sb.append("；午餐 ").append(l.isEmpty() ? "未记录" : l);
        sb.append("；晚餐 ").append(d.isEmpty() ? "未记录" : d);
        if (!s.isEmpty()) sb.append("；加餐 ").append(s);
        return sb.toString();
    }

    private String summarizeImpactOneLine(OrganImpactResp impact) {
        if (impact == null || impact.getImpacts() == null || impact.getImpacts().isEmpty()) return "";
        OrganImpactResp.OrganImpactItem worst = null;
        OrganImpactResp.OrganImpactItem best = null;
        for (var it : impact.getImpacts()) {
            if (it == null) continue;
            if (worst == null || it.getDelta() < worst.getDelta()) worst = it;
            if (best == null || it.getDelta() > best.getDelta()) best = it;
        }
        String w = worst == null ? "" : (String.valueOf(worst.getName()) + (worst.getDeltaText() == null ? "" : (" " + worst.getDeltaText())));
        String g = best == null ? "" : (String.valueOf(best.getName()) + (best.getDeltaText() == null ? "" : (" " + best.getDeltaText())));
        if (w.isEmpty() && g.isEmpty()) return "";
        if (!w.isEmpty() && !g.isEmpty() && w.equals(g)) return w;
        if (!w.isEmpty() && !g.isEmpty()) return "待优化：" + w + "；表现较好：" + g;
        return !w.isEmpty() ? ("待优化：" + w) : ("表现较好：" + g);
    }

    private Long resolveDefaultMemberId(Long userId) {
        if (nutritionService == null || userId == null) return null;
        List<NutritionMemberResp> members;
        try {
            members = nutritionService.listMembers(userId);
        } catch (Exception ignored) {
            members = List.of();
        }
        if (members == null || members.isEmpty()) return null;
        for (var m : members) {
            if (m == null) continue;
            String key = String.valueOf(m.getMemberKey() == null ? "" : m.getMemberKey()).trim();
            if ("me".equalsIgnoreCase(key)) return m.getId();
        }
        for (var m : members) {
            if (m == null) continue;
            if (m.getId() != null) return m.getId();
        }
        return null;
    }

    private boolean isScheduledTrigger(String trigger) {
        String t = String.valueOf(trigger == null ? "" : trigger).trim();
        if (t.isEmpty()) return true;
        if ("on_demand".equalsIgnoreCase(t)) return false;
        if ("manual".equalsIgnoreCase(t)) return false;
        return "scheduled".equalsIgnoreCase(t) || "catchup".equalsIgnoreCase(t) || "cron".equalsIgnoreCase(t) || "auto".equalsIgnoreCase(t);
    }

    private static String formatHHmm(LocalTime t) {
        if (t == null) return "";
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private int safeInt(Map<String, Integer> map, String key) {
        if (map == null || key == null) return 0;
        Integer v = map.get(key);
        return v == null ? 0 : v;
    }

    private double safeDouble(Map<String, Double> map, String key) {
        if (map == null || key == null) return 0.0;
        Double v = map.get(key);
        return v == null ? 0.0 : v;
    }

    private String joinFoodNames(List<NutritionFoodItemResp> foods, int limit) {
        if (foods == null || foods.isEmpty()) return "";
        int max = Math.max(1, Math.min(8, limit));
        List<String> out = new ArrayList<>();
        for (var f : foods) {
            if (f == null) continue;
            String name = String.valueOf(f.getName() == null ? "" : f.getName()).trim();
            if (name.isEmpty()) continue;
            if (!out.contains(name)) out.add(name);
            if (out.size() >= max) break;
        }
        return String.join("、", out);
    }

    private String joinSnackFoodNames(NutritionDayResp day, int limit) {
        if (day == null || day.getSnackCards() == null || day.getSnackCards().isEmpty()) return "";
        int max = Math.max(1, Math.min(8, limit));
        List<String> out = new ArrayList<>();
        for (var c : day.getSnackCards()) {
            if (c == null || c.getFoods() == null) continue;
            for (var f : c.getFoods()) {
                if (f == null) continue;
                String name = String.valueOf(f.getName() == null ? "" : f.getName()).trim();
                if (name.isEmpty()) continue;
                if (!out.contains(name)) out.add(name);
                if (out.size() >= max) break;
            }
            if (out.size() >= max) break;
        }
        return String.join("、", out);
    }

    private String toJsonFoods(List<NutritionFoodItemResp> foods) {
        if (foods == null || foods.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int added = 0;
        for (var f : foods) {
            if (f == null) continue;
            String name = String.valueOf(f.getName() == null ? "" : f.getName()).trim();
            if (name.isEmpty()) continue;
            if (added > 0) sb.append(",");
            sb.append("{\"name\":\"").append(escape(name)).append("\",");
            sb.append("\"unit\":\"").append(escape(String.valueOf(f.getUnit() == null ? "" : f.getUnit()))).append("\",");
            sb.append("\"nutrient\":\"").append(escape(String.valueOf(f.getNutrient() == null ? "" : f.getNutrient()))).append("\"}");
            added++;
            if (added >= 12) break;
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonSnackFoods(NutritionDayResp day) {
        if (day == null || day.getSnackCards() == null || day.getSnackCards().isEmpty()) return "[]";
        List<NutritionFoodItemResp> foods = new ArrayList<>();
        for (var c : day.getSnackCards()) {
            if (c == null || c.getFoods() == null) continue;
            foods.addAll(c.getFoods());
            if (foods.size() >= 20) break;
        }
        return toJsonFoods(foods);
    }

    private String toJsonOrganImpacts(OrganImpactResp impact) {
        if (impact == null || impact.getImpacts() == null || impact.getImpacts().isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int added = 0;
        for (var it : impact.getImpacts()) {
            if (it == null) continue;
            if (added > 0) sb.append(",");
            sb.append("{\"key\":\"").append(escape(String.valueOf(it.getKey()))).append("\",");
            sb.append("\"name\":\"").append(escape(String.valueOf(it.getName()))).append("\",");
            sb.append("\"delta\":").append(it.getDelta()).append(",");
            sb.append("\"deltaText\":\"").append(escape(String.valueOf(it.getDeltaText()))).append("\",");
            sb.append("\"reason\":\"").append(escape(String.valueOf(it.getReason()))).append("\"}");
            added++;
            if (added >= 12) break;
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonIntMap(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i = 0;
        for (var e : map.entrySet()) {
            if (e == null || e.getKey() == null) continue;
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(e.getKey())).append("\":").append(e.getValue() == null ? 0 : e.getValue());
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    private String toJsonDoubleMap(Map<String, Double> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i = 0;
        for (var e : map.entrySet()) {
            if (e == null || e.getKey() == null) continue;
            if (i > 0) sb.append(",");
            double v = e.getValue() == null ? 0.0 : e.getValue();
            sb.append("\"").append(escape(e.getKey())).append("\":").append(String.format("%.1f", v));
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    private String toJsonStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int i = 0;
        for (var s : list) {
            String v = String.valueOf(s == null ? "" : s).trim();
            if (v.isEmpty()) continue;
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(v)).append("\"");
            i++;
            if (i >= 8) break;
        }
        sb.append("]");
        return sb.toString();
    }

    private String escape(String s) {
        String x = String.valueOf(s == null ? "" : s);
        return x.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
