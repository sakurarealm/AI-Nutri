package com.xs.ai.aiheathagent.service;


import com.xs.ai.aiheathagent.dto.req.*;
import com.xs.ai.aiheathagent.dto.resp.*;
import com.xs.ai.aiheathagent.entity.*;
import com.xs.ai.aiheathagent.repo.*;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.NutritionMealRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor
public class HealthService {
    private final UserProfileRepo profileRepo; private final HealthRecordRepo recordRepo;
    private final NutritionMealRecordRepository nutritionMealRecordRepository;
    private final ConversationRecordRepository conversationRecordRepository;


    public ProfileResp getProfile(Long userId){
        var p = profileRepo.findByUserId(userId).orElse(null);
        if (p==null) return new ProfileResp(null, null, null, null, null);
        return new ProfileResp(p.getHeightCm(), p.getTargetWeightKg(), p.getLastWeightKg(), p.getSex(), p.getBirthday());
    }


    @Transactional
    public void updateProfile(Long userId, ProfileUpdateReq req){
        var p = profileRepo.findByUserId(userId).orElseGet(() -> { var np = new UserProfile(); np.setUserId(userId); np.setUpdatedAt(LocalDateTime.now()); return profileRepo.save(np); });
        if (req.getHeightCm()!=null) p.setHeightCm(req.getHeightCm());
        if (req.getTargetWeightKg()!=null) p.setTargetWeightKg(req.getTargetWeightKg());
        if (req.getLastWeightKg()!=null) p.setLastWeightKg(req.getLastWeightKg());
        if (req.getSex()!=null) p.setSex(req.getSex());
        if (req.getBirthday()!=null) p.setBirthday(req.getBirthday());
        p.setUpdatedAt(LocalDateTime.now()); profileRepo.save(p);
    }

    @Transactional
    public RecordItemResp createRecord(Long userId, RecordCreateReq req){
        HealthRecord r = new HealthRecord();
        r.setUserId(userId); r.setType(req.getType()); r.setValue1(req.getValue1()); r.setValue2(req.getValue2());
        r.setUnit1(req.getUnit1()); r.setUnit2(req.getUnit2()); r.setNote(req.getNote()); r.setExt(req.getExt());
        r.setRecordedAt(req.getRecordedAt()!=null? req.getRecordedAt(): LocalDateTime.now());
        r.setCreatedAt(LocalDateTime.now()); r.setUpdatedAt(LocalDateTime.now());
        recordRepo.save(r);


        if ("weight".equalsIgnoreCase(r.getType()) && r.getValue1()!=null){
            var p = profileRepo.findByUserId(userId).orElseGet(() -> { var np = new UserProfile(); np.setUserId(userId); return profileRepo.save(np); });
            p.setLastWeightKg(r.getValue1()); p.setUpdatedAt(LocalDateTime.now()); profileRepo.save(p);
        }
        return new RecordItemResp(r.getId(), r.getType(), r.getValue1(), r.getValue2(), r.getUnit1(), r.getUnit2(), r.getNote(), r.getExt(), r.getRecordedAt());
    }

    @Transactional
    public RecordItemResp updateRecord(Long userId, Long id, RecordUpdateReq req){
        HealthRecord r = recordRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if (!r.getUserId().equals(userId)) throw new IllegalArgumentException("无权修改此记录");
        r.setType(req.getType()); r.setValue1(req.getValue1()); r.setValue2(req.getValue2()); r.setUnit1(req.getUnit1()); r.setUnit2(req.getUnit2());
        r.setNote(req.getNote()); r.setExt(req.getExt());
        if (req.getRecordedAt()!=null) r.setRecordedAt(req.getRecordedAt());
        r.setUpdatedAt(LocalDateTime.now()); recordRepo.save(r);


        if ("weight".equalsIgnoreCase(r.getType()) && r.getValue1()!=null){
            var p = profileRepo.findByUserId(userId).orElseGet(() -> { var np = new UserProfile(); np.setUserId(userId); return profileRepo.save(np); });
// 如果这是最新的一条体重记录，更新 lastWeightKg
            var latest = recordRepo.findTopByUserIdAndTypeOrderByRecordedAtDesc(userId, "weight");
            if (latest.isPresent() && latest.get().getId().equals(r.getId())){
                p.setLastWeightKg(r.getValue1()); p.setUpdatedAt(LocalDateTime.now()); profileRepo.save(p);
            }
        }
        return new RecordItemResp(r.getId(), r.getType(), r.getValue1(), r.getValue2(), r.getUnit1(), r.getUnit2(), r.getNote(), r.getExt(), r.getRecordedAt());
    }

    @Transactional
    public void deleteRecord(Long userId, Long id){
        HealthRecord r = recordRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if (!r.getUserId().equals(userId)) throw new IllegalArgumentException("无权删除此记录");
        recordRepo.delete(r);
    }


    public PageResp<RecordItemResp> listRecords(Long userId, String type, int page, int pageSize){
        Pageable p = PageRequest.of(Math.max(page-1,0), Math.min(Math.max(pageSize,1),100));
        var pg = (type==null || type.isBlank())
                ? recordRepo.findAllByUserIdOrderByRecordedAtDesc(userId, p)
                : recordRepo.findAllByUserIdAndTypeOrderByRecordedAtDesc(userId, type, p);
        var list = pg.getContent().stream().map(r -> new RecordItemResp(r.getId(), r.getType(), r.getValue1(), r.getValue2(), r.getUnit1(), r.getUnit2(), r.getNote(), r.getExt(), r.getRecordedAt())).toList();
        return new PageResp<>(pg.getTotalElements(), list);
    }

    /** 概览：BMI/趋势/连续天数/健康等级 */
    public HealthOverviewResp overview(Long userId){
        var profile = profileRepo.findByUserId(userId).orElse(null);
        BigDecimal heightCm = profile!=null? profile.getHeightCm(): null;
        BigDecimal lastWeight = profile!=null? profile.getLastWeightKg(): null;


// BMI
        BigDecimal bmi = null; String bmiTrend = "flat"; String weightTrend = "flat"; String lastUpdate = "--";
        if (heightCm!=null && lastWeight!=null && heightCm.compareTo(BigDecimal.ZERO)>0){
            BigDecimal h = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP); // m
            bmi = lastWeight.divide(h.multiply(h), 2, RoundingMode.HALF_UP);
        }


// 趋势：取最近两条体重
        var last2 = recordRepo.findAllByUserIdAndTypeAndRecordedAtBetweenOrderByRecordedAtAsc(userId, "weight", LocalDateTime.now().minusYears(5), LocalDateTime.now());
        if (last2!=null && last2.size()>=2){
            var a = last2.get(Math.max(0, last2.size()-2)).getValue1();
            var b = last2.get(last2.size()-1).getValue1();
            if (a!=null && b!=null){
                int cmp = b.compareTo(a);
                if (cmp>0){ weightTrend = "up"; if (bmi!=null) bmiTrend = "up"; }
                else if (cmp<0){ weightTrend = "down"; if (bmi!=null) bmiTrend = "down"; }
            }
            lastUpdate = formatFriendly(last2.get(last2.size()-1).getRecordedAt());
        }


// 连续打卡天数（按 weight 类型统计示例，可扩展为任一类型）
        int continuous = calcContinuousDays(userId, "weight");


// 等级：基于 BMI 简单映射
        String level = levelByBMI(bmi);


        return new HealthOverviewResp(lastUpdate, bmi, bmiTrend, lastWeight, weightTrend, continuous, level);
    }

    /** 体重趋势：周/月/年 */
    public WeightTrendResp weightTrend(Long userId, String range){
        LocalDate today = LocalDate.now();
        LocalDate start;
        DateTimeFormatter labFmt = DateTimeFormatter.ofPattern("M/d");


        if ("year".equalsIgnoreCase(range)){
            start = today.minusYears(1).plusDays(1);
        } else if ("month".equalsIgnoreCase(range)){
            start = today.minusDays(30);
        } else {
            start = today.minusDays(6); // week：含今天在内7天
        }


        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();


        var list = recordRepo.findAllByUserIdAndTypeAndRecordedAtBetweenOrderByRecordedAtAsc(userId, "weight", from, to);


// 按天取“最后一次体重”为该日数值（或取均值均可）
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (var r: list){
            LocalDate d = r.getRecordedAt().toLocalDate();
            if (r.getValue1()!=null) byDay.put(d, r.getValue1());
        }
        List<String> categories = new ArrayList<>();
        List<BigDecimal> series = new ArrayList<>();


        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)){
            categories.add(labFmt.format(d));
            series.add(byDay.getOrDefault(d, null)); // 前端可对 null 做插值/断线
        }


        return new WeightTrendResp(categories, series);
    }

    public HealthSpaceViewResp healthSpaceView(Long userId, int viewIndex){
        int idx = clampInt(viewIndex, 0, 2);
        if (idx == 2) return buildGroupView(idx);

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        List<NutritionMealRecord> meals = nutritionMealRecordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(userId, start, today);
        List<HealthRecord> weights = recordRepo.findAllByUserIdAndTypeAndRecordedAtBetweenOrderByRecordedAtAsc(userId, "weight", from, to);
        List<ConversationRecord> chats = conversationRecordRepository
                .findByUserIdAndMessageTypeAndTimestampBetweenOrderByTimestampDescIdDesc(userId, ConversationRecord.MessageType.USER, from, to, PageRequest.of(0, 5000))
                .getContent();

        boolean hasAny = (meals != null && !meals.isEmpty()) || (weights != null && !weights.isEmpty()) || (chats != null && !chats.isEmpty());
        if (!hasAny) return baselineView(idx);

        Map<LocalDate, List<NutritionMealRecord>> mealsByDay = (meals == null ? List.<NutritionMealRecord>of() : meals).stream()
                .filter(r -> r != null && r.getRecordDate() != null)
                .collect(Collectors.groupingBy(NutritionMealRecord::getRecordDate));
        Set<LocalDate> weightDays = (weights == null ? List.<HealthRecord>of() : weights).stream()
                .filter(r -> r != null && r.getRecordedAt() != null)
                .map(r -> r.getRecordedAt().toLocalDate())
                .collect(Collectors.toSet());
        Set<LocalDate> chatDays = (chats == null ? List.<ConversationRecord>of() : chats).stream()
                .filter(r -> r != null && r.getTimestamp() != null)
                .map(r -> r.getTimestamp().toLocalDate())
                .collect(Collectors.toSet());

        double nutritionToday = scoreNutrition(mealsByDay.get(today));
        double weightToday = weightDays.contains(today) ? 1.0 : 0.0;
        double chatToday = chatDays.contains(today) ? 1.0 : 0.0;

        double nutritionWeekSum = 0.0;
        int weightWeekDays = 0;
        int chatWeekDays = 0;
        int nutritionLoggedDays = 0;
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)){
            double s = scoreNutrition(mealsByDay.get(d));
            nutritionWeekSum += s;
            if (s > 0.0) nutritionLoggedDays++;
            if (weightDays.contains(d)) weightWeekDays++;
            if (chatDays.contains(d)) chatWeekDays++;
        }
        double nutritionWeek = nutritionWeekSum / 7.0;
        double weightWeek = weightWeekDays / 7.0;
        double chatWeek = chatWeekDays / 7.0;

        double completionScore = idx == 0
                ? weighted3(nutritionToday, 0.50, weightToday, 0.30, chatToday, 0.20)
                : weighted3(nutritionWeek, 0.55, weightWeek, 0.30, chatWeek, 0.15);

        int completionRate = clampInt(Math.round((float) (completionScore * 100.0)), 0, 100);
        int streakDays = calcStreakDays(today, mealsByDay, weightDays);
        int warmScore = clampInt(Math.round((float) (60 + completionScore * 30 + Math.min(14, streakDays) * 1.2)), 60, 96);

        String completionHint = buildCompletionHint(idx, completionRate);
        String barLeft = idx == 0 ? "今日节奏" : "本周趋势";
        String barRight = idx == 0 ? ("已完成 " + completionRate + "%") : ("稳定度 " + warmScore + "%");
        String pulseSubtitle = idx == 0 ? "来自同城同阶段用户的匿名汇总" : "更看重趋势，不追逐噪音";

        HealthSpaceViewResp.Pulse pulse = buildPulse(idx, today);
        List<HealthSpaceViewResp.TimelineItem> timeline = idx == 0
                ? buildTodayTimeline(today, mealsByDay.get(today), weights, chats)
                : buildWeekTimeline(start, today, nutritionLoggedDays, streakDays, chats == null ? 0 : chats.size());
        List<HealthSpaceViewResp.Achievement> achievements = buildAchievements(userId, idx, nutritionLoggedDays, streakDays, weights == null ? 0 : weights.size(), chats == null ? 0 : chats.size());

        return new HealthSpaceViewResp(idx, completionRate, completionHint, streakDays, warmScore, pulse, pulseSubtitle, barLeft, barRight, timeline, achievements);
    }


    private String levelByBMI(BigDecimal bmi){
        if (bmi==null) return "Lv.0";
        double v = bmi.doubleValue();
        if (v < 24) return "Lv.4"; // 正常
        if (v < 28) return "Lv.3"; // 超重
        return "Lv.1"; // 肥胖（简单映射，可按业务调）
    }


    private int calcContinuousDays(Long userId, String type){
        LocalDate today = LocalDate.now();
        int days = 0;
        LocalDate d = today;
        while (days < 365) { // 最多统计一年
            LocalDateTime from = d.atStartOfDay();
            LocalDateTime to = d.plusDays(1).atStartOfDay();
            var list = recordRepo.findAllByUserIdAndTypeAndRecordedAtBetweenOrderByRecordedAtAsc(userId, type, from, to);
            if (list==null || list.isEmpty()) break;
            days++; d = d.minusDays(1);
        }
        return days;
    }

    private String formatFriendly(LocalDateTime t){
        if (t==null) return "--";
        LocalDate today = LocalDate.now();
        LocalDate d = t.toLocalDate();
        String hm = t.toLocalTime().withSecond(0).withNano(0).toString(); // HH:mm
        if (d.equals(today)) return "今天 " + hm;
        if (d.equals(today.minusDays(1))) return "昨天 " + hm;
        return d.getMonthValue()+"/"+d.getDayOfMonth()+" "+hm;
    }

    private int clampInt(int n, int min, int max){
        return Math.min(max, Math.max(min, n));
    }

    private double scoreNutrition(List<NutritionMealRecord> list){
        if (list == null || list.isEmpty()) return 0.0;
        Set<NutritionMealType> types = list.stream()
                .filter(Objects::nonNull)
                .map(NutritionMealRecord::getMealType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int main = 0;
        if (types.contains(NutritionMealType.BREAKFAST)) main++;
        if (types.contains(NutritionMealType.LUNCH)) main++;
        if (types.contains(NutritionMealType.DINNER)) main++;
        if (main >= 3) return 1.0;
        if (main == 2) return 0.75;
        if (main == 1) return 0.5;
        return 0.3;
    }

    private double weighted3(double a, double wa, double b, double wb, double c, double wc){
        double sumW = 0.0;
        double sum = 0.0;
        if (wa > 0) { sumW += wa; sum += wa * a; }
        if (wb > 0) { sumW += wb; sum += wb * b; }
        if (wc > 0) { sumW += wc; sum += wc * c; }
        if (sumW <= 0) return 0.0;
        return Math.min(1.0, Math.max(0.0, sum / sumW));
    }

    private int calcStreakDays(LocalDate today, Map<LocalDate, List<NutritionMealRecord>> mealsByDay, Set<LocalDate> weightDays){
        int days = 0;
        for (LocalDate d = today; days < 365; d = d.minusDays(1)){
            double s = scoreNutrition(mealsByDay == null ? null : mealsByDay.get(d));
            boolean ok = s >= 0.5 || (weightDays != null && weightDays.contains(d));
            if (!ok) break;
            days++;
        }
        return days;
    }

    private String buildCompletionHint(int idx, int completionRate){
        if (idx == 1){
            if (completionRate >= 85) return "这一周挺稳，身体会记得这些努力。";
            if (completionRate >= 70) return "不是满分也很好，继续保持节奏。";
            return "小步前进，也在改变体质。";
        }
        if (completionRate >= 85) return "今天的努力，明天会回馈你。";
        if (completionRate >= 70) return "不是满分也很好，继续保持节奏。";
        return "把每一次开始都算作进步。";
    }

    private HealthSpaceViewResp baselineView(int idx){
        int[] baseCompletion = new int[]{78, 86, 72};
        int[] baseStreak = new int[]{5, 12, 7};
        int[] baseWarm = new int[]{83, 88, 79};

        String[] hints = new String[]{
                "不是满分也很好，继续保持节奏。",
                "这一周挺稳，身体会记得这些努力。",
                "你和同频的人群正在一起变好。"
        };
        String[] pulseSubtitle = new String[]{
                "来自同城同阶段用户的匿名汇总",
                "更看重趋势，不追逐噪音",
                "你并不孤单，大家都在做同一件事"
        };
        String[] barLeft = new String[]{"今日节奏", "本周趋势", "人群同频"};

        int completionRate = baseCompletion[idx];
        int warmScore = baseWarm[idx];
        String barRight = idx == 0
                ? ("已完成 " + completionRate + "%")
                : (idx == 1 ? ("稳定度 " + warmScore + "%") : ("同步度 " + clampInt(66 + idx * 7, 0, 100) + "%"));

        HealthSpaceViewResp.Pulse pulse = new HealthSpaceViewResp.Pulse(
                new int[]{1248, 2631, 892}[idx],
                new int[]{6482, 7349, 6021}[idx],
                new int[]{41, 48, 36}[idx]
        );

        List<HealthSpaceViewResp.TimelineItem> timeline = buildBaselineTimeline(idx, completionRate, baseStreak[idx]);
        List<HealthSpaceViewResp.Achievement> achievements = buildAchievements(0L, idx, 0, baseStreak[idx], 0, 0);

        return new HealthSpaceViewResp(
                idx,
                completionRate,
                hints[idx],
                baseStreak[idx],
                warmScore,
                pulse,
                pulseSubtitle[idx],
                barLeft[idx],
                barRight,
                timeline,
                achievements
        );
    }

    private HealthSpaceViewResp buildGroupView(int idx){
        HealthSpaceViewResp base = baselineView(idx);
        LocalDate today = LocalDate.now();
        int reached = 0;
        int activity = 0;
        try {
            long r = nutritionMealRecordRepository.countDistinctUserIdByRecordDate(today);
            if (r > 0) reached = (int) Math.min(Integer.MAX_VALUE, r);
            List<Long> uids = nutritionMealRecordRepository.findDistinctUserIdByRecordDate(today);
            if (uids != null && !uids.isEmpty()){
                double avg = uids.stream().mapToLong(uid -> nutritionMealRecordRepository.countByUserIdAndRecordDate(uid, today)).average().orElse(0.0);
                activity = clampInt((int) Math.round(avg * 1200.0), 0, 99999);
            }
        } catch (Exception ignored) {
        }
        int reachedFinal = reached > 0 ? reached : (base.getPulse() == null ? 0 : base.getPulse().getReached());
        int activityFinal = activity > 0 ? activity : (base.getPulse() == null ? 0 : base.getPulse().getStepsAvg());
        int lightFinal = base.getPulse() == null ? 0 : base.getPulse().getLightMealRate();

        base.setPulse(new HealthSpaceViewResp.Pulse(reachedFinal, activityFinal, lightFinal));
        return base;
    }

    private HealthSpaceViewResp.Pulse buildPulse(int idx, LocalDate today){
        int reached = 0;
        int activity = 0;
        try {
            long r = nutritionMealRecordRepository.countDistinctUserIdByRecordDate(today);
            if (r > 0) reached = (int) Math.min(Integer.MAX_VALUE, r);
            List<Long> uids = nutritionMealRecordRepository.findDistinctUserIdByRecordDate(today);
            if (uids != null && !uids.isEmpty()){
                double avg = uids.stream().mapToLong(uid -> nutritionMealRecordRepository.countByUserIdAndRecordDate(uid, today)).average().orElse(0.0);
                activity = clampInt((int) Math.round(avg * 1200.0), 0, 99999);
            }
        } catch (Exception ignored) {
        }
        int reachedFinal = reached > 0 ? reached : new int[]{1248, 2631, 892}[idx];
        int activityFinal = activity > 0 ? activity : new int[]{6482, 7349, 6021}[idx];
        int lightFinal = new int[]{41, 48, 36}[idx];
        return new HealthSpaceViewResp.Pulse(reachedFinal, activityFinal, lightFinal);
    }

    private List<HealthSpaceViewResp.TimelineItem> buildTodayTimeline(LocalDate today, List<NutritionMealRecord> todayMeals, List<HealthRecord> weights, List<ConversationRecord> chats){
        List<HealthSpaceViewResp.TimelineItem> out = new ArrayList<>();

        if (todayMeals != null && !todayMeals.isEmpty()){
            NutritionMealRecord first = todayMeals.stream().filter(r -> r != null && r.getCreatedAt() != null).min(Comparator.comparing(NutritionMealRecord::getCreatedAt)).orElse(null);
            String t = first != null ? formatHm(first.getCreatedAt()) : "今天";
            int cnt = (int) todayMeals.stream().filter(Objects::nonNull).count();
            int kcal = todayMeals.stream().filter(Objects::nonNull).map(NutritionMealRecord::getCalorie).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            out.add(new HealthSpaceViewResp.TimelineItem(
                    "t-meal",
                    t,
                    "系统",
                    "饮食已记录",
                    "今天已记录 " + cnt + " 条，继续保持。",
                    List.of(new HealthSpaceViewResp.Kv("记录", cnt + " 条"), new HealthSpaceViewResp.Kv("能量", kcal + " kcal"))
            ));
        }

        if (weights != null && !weights.isEmpty()){
            HealthRecord last = weights.stream().filter(r -> r != null && r.getRecordedAt() != null && r.getRecordedAt().toLocalDate().equals(today)).max(Comparator.comparing(HealthRecord::getRecordedAt)).orElse(null);
            if (last != null){
                String t = formatHm(last.getRecordedAt());
                String v = last.getValue1() == null ? "--" : (last.getValue1().stripTrailingZeros().toPlainString() + " kg");
                out.add(new HealthSpaceViewResp.TimelineItem(
                        "t-weight",
                        t,
                        "系统",
                        "体重已记录",
                        "记录不是评判，是对自己更清晰。",
                        List.of(new HealthSpaceViewResp.Kv("体重", v))
                ));
            }
        }

        if (chats != null && !chats.isEmpty()){
            ConversationRecord last = chats.stream().filter(r -> r != null && r.getTimestamp() != null && r.getTimestamp().toLocalDate().equals(today)).max(Comparator.comparing(ConversationRecord::getTimestamp)).orElse(null);
            if (last != null){
                long cnt = chats.stream().filter(r -> r != null && r.getTimestamp() != null && r.getTimestamp().toLocalDate().equals(today)).count();
                out.add(new HealthSpaceViewResp.TimelineItem(
                        "t-ai",
                        formatHm(last.getTimestamp()),
                        "AI",
                        "已和 AI 对话",
                        "把困惑说出来，本身就是一种照顾。",
                        List.of(new HealthSpaceViewResp.Kv("对话", cnt + " 条"))
                ));
            }
        }

        if (out.size() < 3){
            out.add(new HealthSpaceViewResp.TimelineItem(
                    "t-tip",
                    "提醒",
                    "系统",
                    "把晚餐吃得更轻一点",
                    "不用极端，少一点油、多一点蔬菜就好。",
                    List.of(new HealthSpaceViewResp.Kv("建议", "餐后走 8 分钟"))
            ));
        }

        return out.stream().limit(3).toList();
    }

    private List<HealthSpaceViewResp.TimelineItem> buildWeekTimeline(LocalDate start, LocalDate today, int nutritionDays, int streakDays, int chatCount){
        List<HealthSpaceViewResp.TimelineItem> out = new ArrayList<>();
        out.add(new HealthSpaceViewResp.TimelineItem(
                "w-01",
                "本周",
                "AI",
                "节奏更稳定了",
                "更看重趋势，不追逐噪音。",
                List.of(new HealthSpaceViewResp.Kv("饮食记录", nutritionDays + " 天"))
        ));
        out.add(new HealthSpaceViewResp.TimelineItem(
                "w-02",
                "第" + Math.max(1, Math.min(7, streakDays)) + "天",
                "系统",
                "连续达标记录",
                "你把目标拆成了小块，反而更容易坚持。",
                List.of(new HealthSpaceViewResp.Kv("连续", streakDays + " 天"))
        ));
        out.add(new HealthSpaceViewResp.TimelineItem(
                "w-03",
                "本周",
                "AI",
                "把策略变成默认",
                "如果想继续提升，把“蛋白质优先”变成默认策略。",
                List.of(new HealthSpaceViewResp.Kv("对话", chatCount + " 条"))
        ));
        return out;
    }

    private List<HealthSpaceViewResp.TimelineItem> buildBaselineTimeline(int idx, int completionRate, int streakDays){
        if (idx == 0){
            return List.of(
                    new HealthSpaceViewResp.TimelineItem("t-01", "08:12", "系统", "早餐能量更均衡了", "和昨天相比，蛋白质占比上升，饱腹感会更稳定。", List.of(new HealthSpaceViewResp.Kv("蛋白质", "+12%"), new HealthSpaceViewResp.Kv("膳食纤维", "+6g"))),
                    new HealthSpaceViewResp.TimelineItem("t-02", "12:40", "AI", "午后困倦风险降低", "你把主食换成更慢的碳水，血糖波动更温和。", List.of(new HealthSpaceViewResp.Kv("波动", "更平稳"), new HealthSpaceViewResp.Kv("建议", "餐后走 8 分钟"))),
                    new HealthSpaceViewResp.TimelineItem("t-03", "19:18", "系统", "今日饮水接近达标", "还差一点点就满格，睡前留一小杯就好。", List.of(new HealthSpaceViewResp.Kv("完成", completionRate + "%")))
            );
        }
        if (idx == 1){
            return List.of(
                    new HealthSpaceViewResp.TimelineItem("w-01", "本周", "AI", "体重不是唯一指标", "这周你更“稳”：晚餐更轻，睡眠更规律，体感会先于数字改变。", List.of(new HealthSpaceViewResp.Kv("晚餐", "更轻"), new HealthSpaceViewResp.Kv("睡眠", "更稳"))),
                    new HealthSpaceViewResp.TimelineItem("w-02", "第4天", "系统", "连续达标记录刷新", "你把目标拆成了小块，反而更容易坚持。", List.of(new HealthSpaceViewResp.Kv("连续", streakDays + " 天"))),
                    new HealthSpaceViewResp.TimelineItem("w-03", "第6天", "AI", "适应期已过", "如果想继续提升，把“蛋白质优先”变成默认策略。", List.of(new HealthSpaceViewResp.Kv("策略", "先蛋白"), new HealthSpaceViewResp.Kv("提示", "每餐一掌心")))
            );
        }
        return List.of(
                new HealthSpaceViewResp.TimelineItem("g-01", "人群", "匿名", "同阶段的大家在做什么", "更少的“自律”，更多的“顺手”：提前准备、减少选择成本。", List.of(new HealthSpaceViewResp.Kv("共同行为", "提前备餐"), new HealthSpaceViewResp.Kv("微习惯", "餐后散步"))),
                new HealthSpaceViewResp.TimelineItem("g-02", "趋势", "AI", "轻食不等于吃得少", "大家更关注营养密度：蛋白 + 蔬菜 + 慢碳水。", List.of(new HealthSpaceViewResp.Kv("组合", "蛋白+蔬菜+慢碳"))),
                new HealthSpaceViewResp.TimelineItem("g-03", "提醒", "系统", "久坐时间偏长", "很多人用“2 分钟拉伸”替代一次性补救，更容易形成习惯。", List.of(new HealthSpaceViewResp.Kv("建议", "每小时 2 分钟")))
        );
    }

    private String formatHm(LocalDateTime t){
        if (t == null) return "--";
        LocalTime lt = t.toLocalTime().withSecond(0).withNano(0);
        String s = lt.toString();
        if (s.length() >= 5) return s.substring(0, 5);
        return s;
    }

    private List<HealthSpaceViewResp.Achievement> buildAchievements(Long userId, int idx, int nutritionDays, int streakDays, int weightCount, int chatCount){
        List<HealthSpaceViewResp.Achievement> pool = List.of(
                new HealthSpaceViewResp.Achievement("a-01", "🏅", "连续7天轻食"),
                new HealthSpaceViewResp.Achievement("a-02", "🌿", "蛋白质达标"),
                new HealthSpaceViewResp.Achievement("a-03", "💧", "饮水满格"),
                new HealthSpaceViewResp.Achievement("a-04", "👟", "餐后散步"),
                new HealthSpaceViewResp.Achievement("a-05", "🛌", "早睡成就"),
                new HealthSpaceViewResp.Achievement("a-06", "🥗", "蔬菜优先"),
                new HealthSpaceViewResp.Achievement("a-07", "⚡", "能量均衡"),
                new HealthSpaceViewResp.Achievement("a-08", "❤️", "稳定心情"),
                new HealthSpaceViewResp.Achievement("a-09", "🌱", "纤维充足"),
                new HealthSpaceViewResp.Achievement("a-10", "🧘", "深呼吸打卡")
        );

        List<HealthSpaceViewResp.Achievement> out = new ArrayList<>();
        if (nutritionDays >= 1) out.add(new HealthSpaceViewResp.Achievement("u-nutrition", "🥗", "记录饮食"));
        if (weightCount >= 1) out.add(new HealthSpaceViewResp.Achievement("u-weight", "⚖️", "记录体重"));
        if (chatCount >= 1) out.add(new HealthSpaceViewResp.Achievement("u-ai", "🤖", "问AI一次"));
        if (streakDays >= 3) out.add(new HealthSpaceViewResp.Achievement("u-streak", "🏅", "连续达标"));

        int seed = Objects.hash(String.valueOf(userId == null ? 0L : userId), idx, nutritionDays, streakDays, weightCount, chatCount);
        List<HealthSpaceViewResp.Achievement> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random(seed));
        for (HealthSpaceViewResp.Achievement a : shuffled){
            if (out.size() >= 8) break;
            boolean exists = out.stream().anyMatch(x -> x != null && Objects.equals(x.getText(), a.getText()));
            if (!exists) out.add(new HealthSpaceViewResp.Achievement(a.getId(), a.getIcon(), a.getText()));
        }
        return out.stream().limit(8).toList();
    }
}
