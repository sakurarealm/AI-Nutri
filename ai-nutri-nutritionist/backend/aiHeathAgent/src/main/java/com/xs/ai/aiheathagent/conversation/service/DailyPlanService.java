package com.xs.ai.aiheathagent.conversation.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xs.ai.aiheathagent.conversation.model.DailyPlan;
import com.xs.ai.aiheathagent.conversation.model.ExerciseStrategy;
import com.xs.ai.aiheathagent.conversation.model.MealStrategy;
import com.xs.ai.aiheathagent.conversation.model.ReminderItem;
import com.xs.ai.aiheathagent.entity.CapabilityState;
import com.xs.ai.aiheathagent.entity.DailyPlanRecord;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.entity.UserCapabilityProfile;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.entity.UserProfile;
import com.xs.ai.aiheathagent.repo.UserProfileRepo;
import com.xs.ai.aiheathagent.repository.DailyPlanRecordRepository;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import com.xs.ai.aiheathagent.repository.UserCapabilityProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DailyPlanService {
    private final DailyPlanRecordRepository dailyPlanRecordRepository;
    private final UserEventRecordRepository userEventRecordRepository;
    private final UserProfileRepo userProfileRepo;
    private final UserCapabilityProfileRepository userCapabilityProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public DailyPlanService(
            DailyPlanRecordRepository dailyPlanRecordRepository,
            UserEventRecordRepository userEventRecordRepository,
            UserProfileRepo userProfileRepo,
            UserCapabilityProfileRepository userCapabilityProfileRepository
    ) {
        this.dailyPlanRecordRepository = dailyPlanRecordRepository;
        this.userEventRecordRepository = userEventRecordRepository;
        this.userProfileRepo = userProfileRepo;
        this.userCapabilityProfileRepository = userCapabilityProfileRepository;
    }

    public Optional<DailyPlan> findPlan(Long userId, LocalDate date) {
        if (userId == null || date == null) {
            return Optional.empty();
        }
        return dailyPlanRecordRepository.findTopByUserIdAndPlanDateOrderByIdDesc(userId, date).flatMap(r -> {
            try {
                return Optional.of(objectMapper.readValue(r.getPlanJson(), DailyPlan.class));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    @Transactional
    public DailyPlan generateAndPersist(User user, LocalDate planDate) {
        Long userId = user == null ? null : user.getId();
        if (userId == null || planDate == null) {
            throw new IllegalArgumentException("userId/planDate required");
        }
        if (dailyPlanRecordRepository.existsByUserIdAndPlanDate(userId, planDate)) {
            return findPlan(userId, planDate).orElseGet(() -> generate(user, planDate));
        }

        DailyPlan plan = generate(user, planDate);
        String json;
        try {
            json = objectMapper.writeValueAsString(plan);
        } catch (Exception e) {
            json = "{}";
        }

        DailyPlanRecord rec = new DailyPlanRecord();
        rec.setUserId(userId);
        rec.setPlanDate(planDate);
        rec.setPlanId(plan.planId());
        rec.setVariationFlag(plan.variationFlag());
        rec.setPlanJson(json);
        dailyPlanRecordRepository.save(rec);
        return plan;
    }

    public DailyPlan generate(User user, LocalDate planDate) {
        Long userId = user == null ? null : user.getId();
        String nickname = user == null ? "" : String.valueOf(user.getNickname() == null ? "" : user.getNickname()).trim();
        Optional<UserProfile> profile = userId == null ? Optional.empty() : userProfileRepo.findByUserId(userId);
        UserCapabilityProfile cap = userId == null ? null : userCapabilityProfileRepository.findByUserId(userId).orElse(null);
        CapabilityState proactiveReminder = cap == null ? CapabilityState.OFF : safeState(cap.getProactiveReminderEnabled());
        CapabilityState medication = cap == null ? CapabilityState.OFF : safeState(cap.getMedicationReminderEnabled());
        CapabilityState supplement = cap == null ? CapabilityState.OFF : safeState(cap.getSupplementReminderEnabled());
        CapabilityState exerciseEnabled = cap == null ? CapabilityState.OFF : safeState(cap.getExerciseSuggestionEnabled());

        LocalDate from = planDate.minusDays(3);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = planDate.atTime(LocalTime.MAX);
        List<UserEventRecord> events = userId == null ? List.of() : userEventRecordRepository.findByUserIdAndOccurredAtBetweenOrderByOccurredAtAsc(userId, start, end);

        boolean sugarFocus = hasTopic(events, "CONTROL_SUGAR");
        boolean fatLossFocus = hasTopic(events, "FAT_LOSS");

        boolean variationFlag = false;
        try {
            DailyPlanRecord prev = dailyPlanRecordRepository.findTopByUserIdAndPlanDateOrderByIdDesc(userId, planDate.minusDays(1)).orElse(null);
            variationFlag = prev != null;
        } catch (Exception ignored) {
        }

        MealStrategy breakfast = buildBreakfastStrategy(nickname, profile.orElse(null), sugarFocus, fatLossFocus, variationFlag);
        MealStrategy lunch = buildLunchStrategy(nickname, profile.orElse(null), sugarFocus, fatLossFocus, variationFlag);
        MealStrategy dinner = buildDinnerStrategy(nickname, profile.orElse(null), sugarFocus, fatLossFocus, variationFlag);

        List<ReminderItem> reminders = new ArrayList<>();
        boolean passiveOnly = proactiveReminder != CapabilityState.ENABLED;
        if (!passiveOnly) {
            reminders.add(new ReminderItem("meal_breakfast", "MEAL", "早餐提醒", "MORNING"));
            reminders.add(new ReminderItem("meal_lunch", "MEAL", "午餐提醒", "NOON"));
            reminders.add(new ReminderItem("meal_dinner", "MEAL", "晚餐提醒", "EVENING"));
            if (medication == CapabilityState.ENABLED) {
                reminders.add(new ReminderItem("medication_evening", "MEDICATION", "用药提醒", "EVENING"));
            }
            if (supplement == CapabilityState.ENABLED) {
                reminders.add(new ReminderItem("supplement_afternoon", "SUPPLEMENT", "保健品提醒", "AFTERNOON"));
            }
        }

        ExerciseStrategy exercise = null;
        if (exerciseEnabled == CapabilityState.ENABLED) {
            exercise = new ExerciseStrategy(
                    fatLossFocus || sugarFocus,
                    sugarFocus ? "饭后轻活动" : "轻量活动",
                    "15-25 分钟"
            );
        }

        String planId = UUID.randomUUID().toString().replace("-", "");
        return new DailyPlan(planId, planDate, breakfast, lunch, dinner, reminders, exercise, variationFlag, passiveOnly);
    }

    private CapabilityState safeState(CapabilityState s) {
        return s == null ? CapabilityState.OFF : s;
    }

    private boolean hasTopic(List<UserEventRecord> events, String topicKey) {
        if (events == null || events.isEmpty() || topicKey == null || topicKey.isBlank()) {
            return false;
        }
        for (UserEventRecord e : events) {
            String topics = e == null ? "" : String.valueOf(e.getTopics() == null ? "" : e.getTopics());
            if (topics.contains(topicKey)) {
                return true;
            }
        }
        return false;
    }

    private MealStrategy buildBreakfastStrategy(String nickname, UserProfile profile, boolean sugarFocus, boolean fatLossFocus, boolean variationFlag) {
        List<String> rules = new ArrayList<>();
        rules.add("优质蛋白 + 高纤维主食 + 蔬果");
        if (sugarFocus) rules.add("优先低 GI，避免含糖饮料");
        if (fatLossFocus) rules.add("控制油脂与精制碳水");
        if (variationFlag) rules.add("换一种主食或蛋白来源");
        return new MealStrategy("早餐策略", rules);
    }

    private MealStrategy buildLunchStrategy(String nickname, UserProfile profile, boolean sugarFocus, boolean fatLossFocus, boolean variationFlag) {
        List<String> rules = new ArrayList<>();
        rules.add("先吃蔬菜，再吃蛋白，最后主食");
        if (sugarFocus) rules.add("主食减半，选杂粮/薯类");
        if (fatLossFocus) rules.add("避免油炸与浓酱");
        if (variationFlag) rules.add("菜式换花样但结构不变");
        return new MealStrategy("午餐策略", rules);
    }

    private MealStrategy buildDinnerStrategy(String nickname, UserProfile profile, boolean sugarFocus, boolean fatLossFocus, boolean variationFlag) {
        List<String> rules = new ArrayList<>();
        rules.add("清淡为主，七分饱");
        if (sugarFocus) rules.add("晚餐少主食，避免夜宵");
        if (fatLossFocus) rules.add("优先蒸/煮/炖，少炒");
        if (variationFlag) rules.add("优先换蔬菜种类");
        return new MealStrategy("晚餐策略", rules);
    }
}
