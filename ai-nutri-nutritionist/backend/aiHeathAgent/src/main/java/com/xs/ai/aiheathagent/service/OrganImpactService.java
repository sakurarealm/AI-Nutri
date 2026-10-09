package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.dto.resp.NutritionDayResp;
import com.xs.ai.aiheathagent.dto.resp.NutritionFoodItemResp;
import com.xs.ai.aiheathagent.dto.resp.NutritionSnackCardResp;
import com.xs.ai.aiheathagent.dto.resp.OrganDetailResp;
import com.xs.ai.aiheathagent.dto.resp.OrganImpactResp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OrganImpactService {
    private static final Pattern FIBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*克\\s*纤维");
    private static final List<String> SUGAR_KEYWORDS = List.of("奶茶", "可乐", "蛋糕", "甜", "糖", "饼干", "巧克力", "冰淇淋", "含糖");
    private static final List<String> SALTY_KEYWORDS = List.of("泡面", "火腿", "腊", "腌", "咸", "外卖", "辣条", "酱", "卤", "汤底");
    private static final List<String> VEG_KEYWORDS = List.of("西兰花", "菠菜", "沙拉", "青菜", "番茄", "黄瓜", "蘑菇", "菌菇", "羽衣甘蓝", "小番茄", "时蔬", "蔬菜");
    private static final List<String> SPICY_KEYWORDS = List.of("辣", "烧烤");

    private final NutritionService nutritionService;
    private final FoodCatalogService foodCatalogService;
    private final OrganDietAiService organDietAiService;

    public OrganImpactService(NutritionService nutritionService, FoodCatalogService foodCatalogService, OrganDietAiService organDietAiService) {
        this.nutritionService = nutritionService;
        this.foodCatalogService = foodCatalogService;
        this.organDietAiService = organDietAiService;
    }

    @Transactional(readOnly = true)
    public OrganImpactResp getOrganImpact(Long userId, Long memberId, String date) {
        NutritionDayResp day = nutritionService.getDay(userId, memberId, date);
        List<NutritionFoodItemResp> foods = collectFoods(day);

        double fiberG = round1(sumFiberFromFoods(foods));
        int sugarHits = computeKeywordHits(foods, SUGAR_KEYWORDS);
        int saltyHits = computeKeywordHits(foods, SALTY_KEYWORDS);
        int vegHits = computeKeywordHits(foods, VEG_KEYWORDS);
        int omega3Score = computeOmega3Score(foods);
        int snackRisk = computeSnackRisk(day == null ? null : day.getSnackCards());

        Map<String, Double> macroTargets = safeDoubleMap(day == null ? null : day.getMacroTargets());
        Map<String, Double> macroTotals = safeDoubleMap(day == null ? null : day.getMacroTotals());
        Map<String, Integer> macroPercent = computeMacroPercent(macroTotals, macroTargets);

        OrganImpactResp.OrganImpactProfile profile = new OrganImpactResp.OrganImpactProfile(
                String.valueOf(day == null ? "" : day.getDate()),
                foods.size(),
                day == null ? 0 : Math.max(0, day.getCalorieConsumed()),
                day == null ? 2000 : Math.max(0, day.getCalorieTarget()),
                macroPercent,
                macroTotals,
                fiberG,
                sugarHits,
                saltyHits,
                vegHits,
                omega3Score,
                snackRisk
        );

        List<OrganImpactResp.OrganImpactItem> impacts = computeOrganImpacts(profile, foods);
        Map<String, OrganImpactResp.OrganImpactItem> impactByKey = impacts.stream()
                .filter(Objects::nonNull)
                .filter(it -> it.getKey() != null && !it.getKey().isBlank())
                .collect(Collectors.toMap(OrganImpactResp.OrganImpactItem::getKey, it -> it, (a, b) -> a, HashMap::new));

        return new OrganImpactResp(
                day == null ? null : day.getMemberId(),
                String.valueOf(day == null ? "" : day.getDate()),
                profile,
                impacts,
                impactByKey
        );
    }

    @Transactional(readOnly = true)
    public OrganDetailResp getOrganDetail(Long userId, Long memberId, String organKey, String date, boolean refreshAi) {
        String ok = String.valueOf(organKey == null ? "" : organKey).trim();
        if (ok.isEmpty()) ok = "heart";
        OrganImpactResp impactResp = getOrganImpact(userId, memberId, date);
        OrganImpactResp.OrganImpactProfile profile = impactResp == null ? null : impactResp.getProfile();
        List<NutritionFoodItemResp> foods = impactResp == null ? List.of() : collectFoods(nutritionService.getDay(userId, memberId, date));

        OrganImpactResp.OrganImpactItem impact = impactResp == null || impactResp.getImpactByKey() == null ? null : impactResp.getImpactByKey().get(ok);
        OrganMeta meta = organMeta(ok);
        int score = clampScore(baseScore(ok) + (impact == null ? 0 : impact.getDelta()));

        List<OrganDetailResp.Metric> metrics = buildMetrics(ok, profile, foods);
        String summary = buildSummary(ok, profile, impact);
        List<OrganDetailResp.Suggestion> suggestions = buildSuggestions(ok, profile, foods);
        List<OrganDetailResp.Food> recommendations = buildRecommendations(ok, profile, foods);
        List<String> achievements = buildAchievements(profile);
        String dailyTip = buildDailyTip(ok, profile);

        String aiPlan = "";
        if (refreshAi) {
            aiPlan = buildAiPlan(userId, memberId, ok, profile, foods, summary);
        }
        if (aiPlan.isBlank()) {
            aiPlan = buildFallbackAiPlan(ok, profile, foods);
        }

        List<Integer> history = buildHistory7d(userId, memberId, ok, String.valueOf(impactResp == null ? "" : impactResp.getDate()));

        List<OrganDetailResp.OrganTab> tabs = buildTabs(userId, memberId, String.valueOf(impactResp == null ? "" : impactResp.getDate()));

        String dietImpactText = impact == null ? "" : ("饮食影响 " + String.valueOf(impact.getDeltaText()) + " · " + String.valueOf(impact.getReason()));

        return new OrganDetailResp(
                memberId,
                String.valueOf(impactResp == null ? "" : impactResp.getDate()),
                ok,
                new OrganDetailResp.Organ(
                        meta.key(),
                        meta.name(),
                        meta.emoji(),
                        meta.intro(),
                        score,
                        summary,
                        metrics,
                        suggestions,
                        recommendations,
                        dailyTip,
                        aiPlan,
                        achievements
                ),
                history,
                tabs,
                dietImpactText
        );
    }

    private List<NutritionFoodItemResp> collectFoods(NutritionDayResp day) {
        List<NutritionFoodItemResp> out = new ArrayList<>();
        pushList(out, day == null ? null : day.getBreakfastFoods());
        pushList(out, day == null ? null : day.getLunchFoods());
        pushList(out, day == null ? null : day.getDinnerFoods());
        List<NutritionSnackCardResp> cards = day == null ? null : day.getSnackCards();
        if (cards != null) {
            for (NutritionSnackCardResp c : cards) {
                if (c == null) continue;
                pushList(out, c.getFoods());
            }
        }
        return out;
    }

    private void pushList(List<NutritionFoodItemResp> out, List<NutritionFoodItemResp> list) {
        if (out == null || list == null) return;
        for (NutritionFoodItemResp it : list) {
            if (it == null) continue;
            out.add(it);
        }
    }

    private double sumFiberFromFoods(List<NutritionFoodItemResp> foods) {
        if (foods == null) return 0.0;
        double sum = 0.0;
        for (NutritionFoodItemResp f : foods) {
            if (f == null) continue;
            String s = String.valueOf((f.getNutrient() == null || f.getNutrient().isBlank()) ? f.getNote() : f.getNutrient());
            Matcher m = FIBER_PATTERN.matcher(s);
            if (!m.find()) continue;
            double v;
            try {
                v = Double.parseDouble(m.group(1));
            } catch (Exception e) {
                continue;
            }
            sum += v;
        }
        return sum;
    }

    private int computeKeywordHits(List<NutritionFoodItemResp> foods, List<String> keywords) {
        if (foods == null || keywords == null) return 0;
        int hits = 0;
        for (NutritionFoodItemResp f : foods) {
            if (f == null) continue;
            String name = String.valueOf(f.getName() == null ? "" : f.getName());
            String nutrient = String.valueOf(f.getNutrient() == null ? "" : f.getNutrient());
            String text = name + " " + nutrient;
            boolean hit = false;
            for (String k : keywords) {
                if (k == null || k.isBlank()) continue;
                if (text.contains(k)) {
                    hit = true;
                    break;
                }
            }
            if (hit) hits += 1;
        }
        return hits;
    }

    private List<String> matchFoodNames(List<NutritionFoodItemResp> foods, List<String> keywords, int limit) {
        if (foods == null || keywords == null) return List.of();
        int size = Math.min(Math.max(1, limit), 10);
        List<String> out = new ArrayList<>();
        for (NutritionFoodItemResp f : foods) {
            if (f == null) continue;
            String name = String.valueOf(f.getName() == null ? "" : f.getName()).trim();
            String nutrient = String.valueOf(f.getNutrient() == null ? "" : f.getNutrient());
            String note = String.valueOf(f.getNote() == null ? "" : f.getNote());
            String text = name + " " + nutrient + " " + note;
            boolean hit = false;
            for (String k : keywords) {
                if (k == null || k.isBlank()) continue;
                if (text.contains(k)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) continue;
            if (name.isEmpty()) continue;
            if (!out.contains(name)) out.add(name);
            if (out.size() >= size) break;
        }
        return out;
    }

    private String exampleText(List<String> names) {
        if (names == null || names.isEmpty()) return "";
        return "例如：" + String.join("、", names);
    }

    private int computeOmega3Score(List<NutritionFoodItemResp> foods) {
        if (foods == null) return 0;
        int score = 0;
        for (NutritionFoodItemResp f : foods) {
            if (f == null) continue;
            String name = String.valueOf(f.getName() == null ? "" : f.getName());
            String nutrient = String.valueOf(f.getNutrient() == null ? "" : f.getNutrient());
            if (nutrient.contains("Omega-3")) score += 2;
            if (name.contains("三文鱼") || name.contains("深海鱼") || name.contains("金枪鱼")) score += 3;
            if (name.contains("核桃") || name.contains("亚麻籽") || name.contains("奇亚籽")) score += 2;
        }
        if (score < 0) return 0;
        return Math.min(10, score);
    }

    private int computeSnackRisk(List<NutritionSnackCardResp> snackCards) {
        if (snackCards == null) return 0;
        int max = 0;
        for (NutritionSnackCardResp c : snackCards) {
            if (c == null) continue;
            max = Math.max(max, c.getLevel());
        }
        if (max < 0) return 0;
        return Math.min(6, max);
    }

    private Map<String, Integer> computeMacroPercent(Map<String, Double> totals, Map<String, Double> targets) {
        double carbTotal = totals == null ? 0.0 : (totals.get("carb") == null ? 0.0 : totals.get("carb"));
        double proteinTotal = totals == null ? 0.0 : (totals.get("protein") == null ? 0.0 : totals.get("protein"));
        double fatTotal = totals == null ? 0.0 : (totals.get("fat") == null ? 0.0 : totals.get("fat"));

        double carbTarget = targets == null ? 0.0 : (targets.get("carb") == null ? 0.0 : targets.get("carb"));
        double proteinTarget = targets == null ? 0.0 : (targets.get("protein") == null ? 0.0 : targets.get("protein"));
        double fatTarget = targets == null ? 0.0 : (targets.get("fat") == null ? 0.0 : targets.get("fat"));

        return Map.of(
                "carb", computePct(carbTotal, carbTarget),
                "fat", computePct(fatTotal, fatTarget),
                "protein", computePct(proteinTotal, proteinTarget)
        );
    }

    private int computePct(double actual, double target) {
        double t = target <= 0 ? 1.0 : target;
        int pct = (int) Math.round((actual / t) * 100.0);
        if (pct < 0) return 0;
        return Math.min(200, pct);
    }

    private Map<String, Double> safeDoubleMap(Map<String, Double> m) {
        if (m == null) return Map.of();
        return new HashMap<>(m);
    }

    private List<OrganImpactResp.OrganImpactItem> computeOrganImpacts(OrganImpactResp.OrganImpactProfile profile, List<NutritionFoodItemResp> foods) {
        OrganImpactResp.OrganImpactProfile p = profile == null ? new OrganImpactResp.OrganImpactProfile() : profile;

        double ratio = p.getCalorieTarget() > 0 ? ((double) p.getCalorieConsumed() / (double) p.getCalorieTarget()) : 0.0;
        int carbPct = safeInt(p.getMacroPercent(), "carb");
        int fatPct = safeInt(p.getMacroPercent(), "fat");
        int proteinPct = safeInt(p.getMacroPercent(), "protein");
        double fiber = p.getFiberG();
        int sugar = p.getSugarHits();
        int salty = p.getSaltyHits();
        int veg = p.getVegHits();
        int omega3 = p.getOmega3Score();
        int snackRisk = p.getSnackRisk();
        boolean spicyHit = computeKeywordHits(foods, List.of("辣", "烧烤")) > 0;

        List<OrganImpactResp.OrganImpactItem> impacts = new ArrayList<>();

        int heartDelta = clampDelta((omega3 >= 4 ? 3 : omega3 >= 2 ? 2 : 0) + (salty >= 2 ? -4 : salty == 1 ? -2 : 2) + (ratio > 1.12 ? -2 : ratio < 0.75 ? -1 : 0));
        impacts.add(new OrganImpactResp.OrganImpactItem(
                "heart",
                "心脏",
                "🫀",
                heartDelta,
                deltaText(heartDelta),
                level(heartDelta),
                salty > 0 ? "盐分偏高会拉低心血管表现" : (omega3 > 0 ? "Omega-3 有加分，控盐也不错" : "控盐表现不错，继续保持")
        ));

        int liverDelta = clampDelta((fiber >= 25 ? 3 : fiber >= 15 ? 1 : -2) + (sugar >= 1 ? -3 : 1) + (fatPct > 120 ? -2 : 0));
        impacts.add(new OrganImpactResp.OrganImpactItem(
                "liver",
                "肝脏",
                "🧬",
                liverDelta,
                deltaText(liverDelta),
                level(liverDelta),
                sugar > 0 ? "糖/精制摄入会增加代谢负担" : (fiber >= 15 ? "纤维有帮助，代谢压力更小" : "纤维偏少，建议多吃蔬菜全谷")
        ));

        int kidneyDelta = clampDelta((salty >= 1 ? -3 : 2) + (proteinPct > 140 ? -1 : 0) + (ratio > 1.2 ? -1 : 0));
        impacts.add(new OrganImpactResp.OrganImpactItem(
                "kidney",
                "肾",
                "💧",
                kidneyDelta,
                deltaText(kidneyDelta),
                level(kidneyDelta),
                salty > 0 ? "重口会让控盐更关键" : "整体较清淡，对肾更友好"
        ));

        int brainDelta = clampDelta((omega3 >= 3 ? 3 : omega3 >= 1 ? 1 : 0) + (sugar >= 2 ? -2 : 1) + (veg >= 2 ? 1 : 0));
        impacts.add(new OrganImpactResp.OrganImpactItem(
                "brain",
                "大脑",
                "🧠",
                brainDelta,
                deltaText(brainDelta),
                level(brainDelta),
                omega3 > 0 ? "脂肪酸摄入有利于专注与认知" : "可加一点坚果/深海鱼更友好"
        ));

        int pancreasDelta = clampDelta((carbPct > 130 ? -4 : carbPct > 115 ? -2 : 1) + (sugar >= 1 ? -2 : 1) + (fiber >= 25 ? 2 : 0));
        impacts.add(new OrganImpactResp.OrganImpactItem(
                "pancreas",
                "胰腺",
                "🍯",
                pancreasDelta,
                deltaText(pancreasDelta),
                level(pancreasDelta),
                (carbPct > 120 || sugar > 0) ? "精制碳水/甜食会放大波动" : "碳水更平稳，搭配得不错"
        ));

        int intestineDelta = clampDelta((fiber >= 25 ? 4 : fiber >= 15 ? 2 : fiber >= 10 ? 0 : -3) + (veg >= 2 ? 1 : 0));
        impacts.add(new OrganImpactResp.OrganImpactItem(
                "intestine",
                "肠道",
                "🥬",
                intestineDelta,
                deltaText(intestineDelta),
                level(intestineDelta),
                fiber >= 15 ? "纤维更足，菌群更开心" : "纤维不足，建议每餐加蔬菜"
        ));

        int stomachDelta = clampDelta((snackRisk >= 4 ? -2 : snackRisk >= 2 ? -1 : 1) + (spicyHit ? -1 : 0));
        impacts.add(new OrganImpactResp.OrganImpactItem(
                "stomach",
                "胃",
                "🍲",
                stomachDelta,
                deltaText(stomachDelta),
                level(stomachDelta),
                snackRisk >= 2 ? "加餐偏多会干扰消化节律" : "节律更规律，对胃更友好"
        ));

        impacts.sort(Comparator.comparingInt(OrganImpactResp.OrganImpactItem::getDelta).reversed());
        return impacts;
    }

    private int safeInt(Map<String, Integer> map, String key) {
        if (map == null || key == null) return 0;
        Integer v = map.get(key);
        return v == null ? 0 : v;
    }

    private int clampDelta(int d) {
        if (d < -8) return -8;
        if (d > 8) return 8;
        return d;
    }

    private String deltaText(int d) {
        String fmt = d > 0 ? ("+" + d) : String.valueOf(d);
        return fmt + "分";
    }

    private String level(int d) {
        if (d >= 2) return "good";
        if (d <= -2) return "bad";
        return "neutral";
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private int clampScore(int score) {
        if (score < 0) return 0;
        return Math.min(100, score);
    }

    private record OrganMeta(String key, String name, String emoji, String intro) {
    }

    private OrganMeta organMeta(String key) {
        String k = String.valueOf(key == null ? "" : key).trim();
        return switch (k) {
            case "brain" -> new OrganMeta("brain", "大脑", "🧠", "大脑负责思维、记忆与情绪。饮食结构会影响能量稳定、炎症水平与专注感。");
            case "heart" -> new OrganMeta("heart", "心脏", "🫀", "心脏是循环系统核心。盐分、脂肪结构与能量平衡会影响心血管负担。");
            case "liver" -> new OrganMeta("liver", "肝脏", "🧬", "肝脏参与代谢与营养储存。高糖高脂与纤维不足会增加代谢压力。");
            case "stomach" -> new OrganMeta("stomach", "胃", "🍲", "胃负责食物初步消化。加餐频繁、辛辣刺激与能量过载会影响消化节律。");
            case "intestine" -> new OrganMeta("intestine", "肠道", "🥬", "肠道承担营养吸收与菌群平衡。纤维、多样化与糖负担会影响肠道状态。");
            case "pancreas" -> new OrganMeta("pancreas", "胰腺", "🍯", "胰腺参与血糖调节。精制碳水与含糖加餐会放大餐后波动。");
            case "kidney" -> new OrganMeta("kidney", "肾", "💧", "肾脏负责体液与电解质平衡。高盐与极端高蛋白会增加负担。");
            case "lungs" -> new OrganMeta("lungs", "肺部", "🫁", "肺部进行气体交换。抗氧化与抗炎饮食能支持呼吸系统状态。");
            case "eyes" -> new OrganMeta("eyes", "眼睛", "👁️", "视疲劳与营养结构有关。抗氧化、Omega-3 与控糖更友好。");
            default -> new OrganMeta("heart", "心脏", "🫀", "心脏是循环系统核心。盐分、脂肪结构与能量平衡会影响心血管负担。");
        };
    }

    private int baseScore(String organKey) {
        String k = String.valueOf(organKey == null ? "" : organKey).trim();
        return switch (k) {
            case "heart" -> 72;
            case "brain" -> 68;
            case "liver" -> 64;
            case "kidney" -> 70;
            case "pancreas" -> 66;
            case "intestine" -> 62;
            case "stomach" -> 68;
            case "lungs" -> 74;
            case "eyes" -> 72;
            default -> 70;
        };
    }

    private List<OrganDetailResp.Metric> buildMetrics(String organKey, OrganImpactResp.OrganImpactProfile profile, List<NutritionFoodItemResp> foods) {
        OrganImpactResp.OrganImpactProfile p = profile == null ? new OrganImpactResp.OrganImpactProfile() : profile;
        double ratio = p.getCalorieTarget() > 0 ? ((double) p.getCalorieConsumed() / (double) p.getCalorieTarget()) : 0.0;
        int carbPct = safeInt(p.getMacroPercent(), "carb");
        int fatPct = safeInt(p.getMacroPercent(), "fat");
        int proteinPct = safeInt(p.getMacroPercent(), "protein");
        double fiber = p.getFiberG();
        int sugar = p.getSugarHits();
        int salty = p.getSaltyHits();
        int veg = p.getVegHits();
        int omega3 = p.getOmega3Score();
        int snackRisk = p.getSnackRisk();
        boolean spicyHit = computeKeywordHits(foods, SPICY_KEYWORDS) > 0;

        List<String> saltyExamples = matchFoodNames(foods, SALTY_KEYWORDS, 3);
        List<String> sugarExamples = matchFoodNames(foods, SUGAR_KEYWORDS, 3);
        List<String> vegExamples = matchFoodNames(foods, VEG_KEYWORDS, 3);
        List<String> spicyExamples = matchFoodNames(foods, SPICY_KEYWORDS, 3);
        List<String> omegaExamples = matchFoodNames(foods, List.of("三文鱼", "深海鱼", "金枪鱼", "核桃", "亚麻籽", "奇亚籽", "Omega-3"), 3);

        List<OrganDetailResp.Metric> list = new ArrayList<>();

        if ("heart".equals(organKey) || "kidney".equals(organKey)) {
            list.add(metricSalt(salty, saltyExamples));
        }
        if ("heart".equals(organKey) || "brain".equals(organKey) || "eyes".equals(organKey)) {
            list.add(metricOmega3(omega3, omegaExamples));
        }
        if ("liver".equals(organKey) || "intestine".equals(organKey) || "pancreas".equals(organKey) || "eyes".equals(organKey) || "brain".equals(organKey)) {
            list.add(metricSugarLoad(sugar, carbPct, sugarExamples));
        }
        if ("liver".equals(organKey) || "intestine".equals(organKey) || "pancreas".equals(organKey)) {
            list.add(metricFiber(fiber));
        }
        if ("pancreas".equals(organKey)) {
            list.add(metricCarbBalance(carbPct, fiber, sugar));
        }
        if ("kidney".equals(organKey)) {
            list.add(metricProteinLoad(proteinPct));
        }
        if ("stomach".equals(organKey)) {
            list.add(metricSnack(snackRisk));
            list.add(metricSpicy(spicyHit, spicyExamples));
        }
        if ("lungs".equals(organKey) || "eyes".equals(organKey) || "brain".equals(organKey)) {
            list.add(metricVeg(veg, vegExamples));
        }

        list.add(metricEnergyBalance(ratio));
        if ("heart".equals(organKey) || "liver".equals(organKey)) {
            list.add(metricFatBalance(fatPct));
        }

        return list.stream().limit(6).toList();
    }

    private OrganDetailResp.Metric metricSalt(int saltyHits, List<String> examples) {
        boolean bad = saltyHits >= 2;
        boolean neutral = saltyHits == 1;
        String status = bad ? "bad" : neutral ? "neutral" : "good";
        int percentage = bad ? 55 : neutral ? 68 : 85;
        String value = bad ? "偏高" : neutral ? "稍高" : "适中";
        String ex = exampleText(examples);
        String detail = bad
                ? ("今日饮食偏咸（加工/外卖/汤底命中较多）。" + (ex.isEmpty() ? "" : (" " + ex)) + " 下一餐把汤底/酱料减半，少腌制。")
                : neutral
                ? ("盐分略高，下一餐尽量清淡。" + (ex.isEmpty() ? "" : (" " + ex)))
                : "控盐不错，继续保持清淡烹饪。";
        return new OrganDetailResp.Metric("控盐表现", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricOmega3(int omega3Score, List<String> examples) {
        double grams = omega3Score >= 6 ? 1.6 : omega3Score >= 3 ? 1.1 : omega3Score >= 1 ? 0.7 : 0.3;
        String status = grams >= 1.4 ? "good" : grams >= 0.8 ? "neutral" : "bad";
        int percentage = grams >= 1.4 ? 82 : grams >= 0.8 ? 68 : 52;
        String ex = exampleText(examples);
        String detail = grams >= 1.2
                ? ("Omega-3 更理想，有利于心脑与视网膜支持。" + (ex.isEmpty() ? "" : (" " + ex)))
                : ("可在下一餐加一份深海鱼或一小把坚果。" + (ex.isEmpty() ? "" : (" " + ex)));
        return new OrganDetailResp.Metric("Omega-3 结构", String.valueOf(grams), "g", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricFiber(double fiberG) {
        String status = fiberG >= 25 ? "good" : fiberG >= 15 ? "neutral" : "bad";
        int percentage = fiberG >= 25 ? 88 : fiberG >= 15 ? 70 : 52;
        String value = fiberG >= 25 ? "充足" : fiberG >= 15 ? "一般" : "偏低";
        String detail = fiberG >= 25 ? "纤维达标，有助于代谢与肠道菌群。" : "纤维偏少，建议每餐加蔬菜/豆类/全谷物。";
        return new OrganDetailResp.Metric("膳食纤维", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricSugarLoad(int sugarHits, int carbPct, List<String> examples) {
        boolean bad = sugarHits >= 1 || carbPct > 120;
        boolean neutral = !bad && carbPct > 110;
        String status = bad ? "bad" : neutral ? "neutral" : "good";
        int percentage = bad ? 56 : neutral ? 68 : 82;
        String value = bad ? "偏高" : neutral ? "中等" : "较少";
        String ex = exampleText(examples);
        String detail = bad ? "今天含糖/精制相关食物命中较多，建议用水果/酸奶/坚果替代甜饮甜点。"
                : neutral ? "碳水略偏高，下一餐把主食减 1/4 并加一份蔬菜。"
                : "精制负担较低，保持主食与蛋白/蔬菜同餐。";
        if (!ex.isEmpty() && (bad || neutral)) detail = detail + " " + ex;
        return new OrganDetailResp.Metric("精制负担", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricCarbBalance(int carbPct, double fiberG, int sugarHits) {
        boolean bad = carbPct > 130 || sugarHits >= 1;
        boolean neutral = !bad && carbPct > 115;
        String status = bad ? "bad" : neutral ? "neutral" : "good";
        int percentage = bad ? 55 : neutral ? 68 : 84;
        String value = bad ? "波动风险" : neutral ? "略偏高" : "更稳";
        String detail = bad ? "主食偏多或含糖命中，建议主食搭配蛋白+蔬菜，并优先全谷。"
                : (fiberG >= 15 ? "搭配不错，继续让蔬菜与蛋白先于主食。" : "可加一份蔬菜/豆类提高纤维，降低波动。");
        return new OrganDetailResp.Metric("碳水稳定性", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricProteinLoad(int proteinPct) {
        boolean bad = proteinPct > 160;
        boolean neutral = proteinPct > 135 && !bad;
        String status = bad ? "neutral" : neutral ? "neutral" : "good";
        int percentage = bad ? 62 : neutral ? 72 : 84;
        String value = bad ? "偏高" : neutral ? "稍高" : "适中";
        String detail = bad ? "蛋白目标超出较多，若长期极高蛋白不友好，建议分散到三餐并优先优质蛋白。"
                : neutral ? "蛋白略高，注意搭配蔬菜与足量饮水。" : "蛋白结构更平衡，继续保持。";
        return new OrganDetailResp.Metric("蛋白负担", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricSnack(int snackRisk) {
        boolean bad = snackRisk >= 4;
        boolean neutral = snackRisk >= 2 && !bad;
        String status = bad ? "bad" : neutral ? "neutral" : "good";
        int percentage = bad ? 58 : neutral ? 70 : 84;
        String value = bad ? "偏多" : neutral ? "有点多" : "克制";
        String detail = bad ? "加餐偏多会干扰消化节律，建议把加餐固定在下午并选择无糖酸奶/坚果。"
                : neutral ? "加餐稍多，尽量把甜饮甜点换成水果/坚果。" : "加餐更克制，对消化更友好。";
        return new OrganDetailResp.Metric("加餐频率", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricSpicy(boolean spicyHit, List<String> examples) {
        String status = spicyHit ? "neutral" : "good";
        int percentage = spicyHit ? 70 : 86;
        String value = spicyHit ? "有刺激" : "温和";
        String ex = exampleText(examples);
        String detail = spicyHit
                ? ("辛辣/烧烤命中，若有反酸/胃不适，下一餐尽量清淡少辣。" + (ex.isEmpty() ? "" : (" " + ex)))
                : "刺激性较少，更利于胃部节律。";
        return new OrganDetailResp.Metric("刺激负担", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricVeg(int vegHits, List<String> examples) {
        String status = vegHits >= 2 ? "good" : vegHits >= 1 ? "neutral" : "bad";
        int percentage = vegHits >= 2 ? 88 : vegHits >= 1 ? 72 : 55;
        String value = vegHits >= 2 ? "充足" : vegHits >= 1 ? "一般" : "偏少";
        String ex = exampleText(examples);
        String detail = vegHits >= 1
                ? ("蔬菜/深色食物命中，有助于抗氧化与微量营养素。" + (ex.isEmpty() ? "" : (" " + ex)))
                : "蔬菜偏少，建议每餐加一份深色蔬菜。";
        return new OrganDetailResp.Metric("抗氧化食物", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricEnergyBalance(double ratio) {
        boolean bad = ratio > 1.12;
        boolean neutral = ratio >= 0.95 && ratio <= 1.12;
        String status = bad ? "neutral" : neutral ? "good" : "neutral";
        int percentage = bad ? 65 : neutral ? 82 : 72;
        String value = bad ? "偏超" : neutral ? "接近目标" : "偏少";
        String detail = bad ? "今日能量可能超出目标，下一餐可减少油炸与高脂零食。" : neutral ? "能量更接近目标，保持即可。" : "能量偏少，注意不要长期过低，优先补充优质蛋白与主食。";
        return new OrganDetailResp.Metric("能量平衡", value, "", percentage, status, detail);
    }

    private OrganDetailResp.Metric metricFatBalance(int fatPct) {
        boolean bad = fatPct > 135;
        boolean neutral = fatPct > 115 && !bad;
        String status = bad ? "neutral" : neutral ? "neutral" : "good";
        int percentage = bad ? 62 : neutral ? 72 : 84;
        String value = bad ? "偏高" : neutral ? "稍高" : "适中";
        String detail = bad ? "脂肪目标超出较多，下一餐少油少炸，优先蒸煮炖。" : neutral ? "脂肪略高，注意酱料/坚果份量。" : "脂肪结构更平衡，继续保持。";
        return new OrganDetailResp.Metric("脂肪占比", value, "", percentage, status, detail);
    }

    private String buildSummary(String organKey, OrganImpactResp.OrganImpactProfile profile, OrganImpactResp.OrganImpactItem impact) {
        OrganImpactResp.OrganImpactProfile p = profile == null ? new OrganImpactResp.OrganImpactProfile() : profile;
        if (impact != null && impact.getReason() != null && !impact.getReason().isBlank()) {
            return String.valueOf(impact.getReason());
        }
        if ("pancreas".equals(organKey)) {
            int carbPct = safeInt(p.getMacroPercent(), "carb");
            return carbPct > 120 ? "碳水偏高，餐后波动风险增加" : "碳水更稳，继续保持同餐搭配";
        }
        if ("intestine".equals(organKey)) {
            return p.getFiberG() >= 15 ? "纤维不错，肠道更开心" : "纤维偏少，菌群支持不足";
        }
        if ("heart".equals(organKey) || "kidney".equals(organKey)) {
            return p.getSaltyHits() > 0 ? "盐分需要关注，控盐更关键" : "饮食更清淡，对循环更友好";
        }
        return "饮食结构会影响指标表现";
    }

    private List<OrganDetailResp.Suggestion> buildSuggestions(String organKey, OrganImpactResp.OrganImpactProfile profile, List<NutritionFoodItemResp> foods) {
        OrganImpactResp.OrganImpactProfile p = profile == null ? new OrganImpactResp.OrganImpactProfile() : profile;
        int carbPct = safeInt(p.getMacroPercent(), "carb");
        int fatPct = safeInt(p.getMacroPercent(), "fat");
        double fiber = p.getFiberG();
        int sugar = p.getSugarHits();
        int salty = p.getSaltyHits();
        int omega3 = p.getOmega3Score();
        int snackRisk = p.getSnackRisk();
        boolean spicyHit = computeKeywordHits(foods, List.of("辣", "烧烤")) > 0;

        List<OrganDetailResp.Suggestion> out = new ArrayList<>();

        if (salty > 0 && ("heart".equals(organKey) || "kidney".equals(organKey))) {
            out.add(new OrganDetailResp.Suggestion("food", "下一餐减盐", "把汤底/酱料减半，外卖少点酱与腌制"));
        }
        if (omega3 < 3 && ("heart".equals(organKey) || "brain".equals(organKey) || "eyes".equals(organKey))) {
            out.add(new OrganDetailResp.Suggestion("food", "补一份 Omega-3", "加深海鱼或一小把核桃/亚麻籽，支持心脑与视力"));
        }
        if ((sugar > 0 || carbPct > 120) && ("pancreas".equals(organKey) || "liver".equals(organKey) || "eyes".equals(organKey) || "brain".equals(organKey))) {
            out.add(new OrganDetailResp.Suggestion("food", "控糖换替代", "甜饮甜点换成无糖酸奶/水果/坚果，主食减 1/4 并加蔬菜"));
        }
        if (fiber < 15 && ("liver".equals(organKey) || "intestine".equals(organKey) || "pancreas".equals(organKey))) {
            out.add(new OrganDetailResp.Suggestion("food", "每餐加纤维", "加一拳蔬菜或一份豆类/全谷，提升纤维并更稳"));
        }
        if (fatPct > 135 && ("heart".equals(organKey) || "liver".equals(organKey))) {
            out.add(new OrganDetailResp.Suggestion("food", "少油少炸", "下一餐优先蒸/煮/炖，避免油炸与重口酱料"));
        }
        if ("stomach".equals(organKey)) {
            if (snackRisk >= 2) out.add(new OrganDetailResp.Suggestion("lifestyle", "固定加餐窗口", "把加餐固定在下午，夜间尽量不加餐"));
            if (spicyHit) out.add(new OrganDetailResp.Suggestion("food", "降低刺激", "辛辣/烧烤少一点，主食搭配温和蛋白更友好"));
        }
        if (out.isEmpty()) {
            out.add(new OrganDetailResp.Suggestion("food", "保持结构", "主食+蛋白+蔬菜同餐，优先原型食物"));
            out.add(new OrganDetailResp.Suggestion("lifestyle", "规律三餐", "按时吃饭，避免暴食与长时间空腹"));
        }
        return out.stream().limit(3).toList();
    }

    private List<OrganDetailResp.Food> buildRecommendations(String organKey, OrganImpactResp.OrganImpactProfile profile, List<NutritionFoodItemResp> foods) {
        OrganImpactResp.OrganImpactProfile p = profile == null ? new OrganImpactResp.OrganImpactProfile() : profile;
        int carbPct = safeInt(p.getMacroPercent(), "carb");
        double fiber = p.getFiberG();
        int sugar = p.getSugarHits();
        int salty = p.getSaltyHits();
        int omega3 = p.getOmega3Score();

        List<String> names = new ArrayList<>();
        if ((omega3 < 3) && ("heart".equals(organKey) || "brain".equals(organKey) || "eyes".equals(organKey))) {
            names.add("三文鱼");
            names.add("核桃");
        }
        if ((fiber < 15) && ("intestine".equals(organKey) || "liver".equals(organKey) || "pancreas".equals(organKey))) {
            names.add("西兰花");
            names.add("燕麦");
        }
        if ((sugar > 0 || carbPct > 120) && ("pancreas".equals(organKey) || "liver".equals(organKey) || "eyes".equals(organKey))) {
            names.add("无糖豆浆");
            names.add("希腊酸奶(无糖)");
        }
        if ((salty > 0) && ("heart".equals(organKey) || "kidney".equals(organKey))) {
            names.add("黄瓜");
            names.add("番茄");
        }
        if ("stomach".equals(organKey)) {
            names.add("白粥+咸菜(简)");
            names.add("酸奶");
        }
        if ("lungs".equals(organKey)) {
            names.add("菠菜");
            names.add("番茄");
        }
        if ("eyes".equals(organKey)) {
            names.add("蓝莓");
            names.add("菠菜");
        }
        if (names.isEmpty()) {
            names.add("西兰花");
            names.add("鸡胸肉");
        }

        List<String> distinct = names.stream().filter(s -> s != null && !s.isBlank()).distinct().limit(3).toList();
        List<OrganDetailResp.Food> out = new ArrayList<>();
        for (String name : distinct) {
            String img = "";
            try {
                if (foodCatalogService != null) {
                    var list = foodCatalogService.search(name, 1);
                    if (list != null && !list.isEmpty() && list.get(0) != null) {
                        img = String.valueOf(list.get(0).getImg() == null ? "" : list.get(0).getImg());
                    }
                }
            } catch (Exception ignored) {
                img = "";
            }
            out.add(new OrganDetailResp.Food(name, img, recommendNoteByName(name)));
        }
        return out;
    }

    private String recommendNoteByName(String name) {
        String n = String.valueOf(name == null ? "" : name);
        if (n.contains("三文鱼") || n.contains("金枪鱼")) return "Omega-3";
        if (n.contains("核桃") || n.contains("亚麻籽")) return "健康脂肪";
        if (n.contains("西兰花") || n.contains("菠菜") || n.contains("番茄")) return "抗氧化";
        if (n.contains("燕麦")) return "全谷纤维";
        if (n.contains("酸奶")) return "发酵乳";
        if (n.contains("黄瓜")) return "清淡低盐";
        if (n.contains("豆浆")) return "植物蛋白";
        if (n.contains("蓝莓")) return "花青素";
        return "更友好";
    }

    private List<String> buildAchievements(OrganImpactResp.OrganImpactProfile profile) {
        OrganImpactResp.OrganImpactProfile p = profile == null ? new OrganImpactResp.OrganImpactProfile() : profile;
        List<String> list = new ArrayList<>();
        if (p.getSugarHits() <= 0) list.add("今日无含糖命中");
        if (p.getFiberG() >= 25) list.add("纤维达标");
        if (p.getSaltyHits() <= 0) list.add("控盐优秀");
        if (p.getOmega3Score() >= 3) list.add("Omega-3 加分");
        if (p.getSnackRisk() <= 1) list.add("加餐更克制");
        if (list.isEmpty()) list.add("已记录饮食数据");
        return list.stream().limit(3).toList();
    }

    private String buildDailyTip(String organKey, OrganImpactResp.OrganImpactProfile profile) {
        OrganImpactResp.OrganImpactProfile p = profile == null ? new OrganImpactResp.OrganImpactProfile() : profile;
        if ("intestine".equals(organKey)) {
            return p.getFiberG() >= 15 ? "今天纤维不错，继续让每餐都有一份蔬菜。" : "今天给每餐加一份蔬菜，会明显改善肠道状态。";
        }
        if ("pancreas".equals(organKey)) {
            return "一顿饭里先菜后肉再主食，更稳。";
        }
        if ("heart".equals(organKey) || "kidney".equals(organKey)) {
            return p.getSaltyHits() > 0 ? "下一餐少点酱料与汤底，控盐更关键。" : "清淡烹饪更友好，保持即可。";
        }
        if ("eyes".equals(organKey)) {
            return "今天加一份深色蔬菜或浆果，抗氧化更友好。";
        }
        if ("lungs".equals(organKey)) {
            return "多一份蔬菜水果与健康脂肪，支持抗氧化与抗炎。";
        }
        if ("stomach".equals(organKey)) {
            return "把夜间加餐换成温水/无糖酸奶更友好。";
        }
        return "保持主食、蛋白、蔬菜同餐结构。";
    }

    private String buildAiPlan(Long userId, Long memberId, String organKey, OrganImpactResp.OrganImpactProfile profile, List<NutritionFoodItemResp> foods, String summary) {
        if (organDietAiService == null) return "";
        OrganImpactResp.OrganImpactProfile p = profile == null ? new OrganImpactResp.OrganImpactProfile() : profile;
        List<String> foodNames = foods == null ? List.of() : foods.stream()
                .filter(Objects::nonNull)
                .map(NutritionFoodItemResp::getName)
                .map(s -> String.valueOf(s == null ? "" : s).trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(10)
                .toList();

        String ctx = "器官：" + organMeta(organKey).name()
                + "\n日期：" + String.valueOf(p.getDate() == null ? "" : p.getDate())
                + "\n四大数据：热量 " + p.getCalorieConsumed() + "/" + p.getCalorieTarget()
                + "，碳水%" + safeInt(p.getMacroPercent(), "carb")
                + "，蛋白%" + safeInt(p.getMacroPercent(), "protein")
                + "，脂肪%" + safeInt(p.getMacroPercent(), "fat")
                + "\n食物：" + String.join("、", foodNames)
                + "\n当前结论：" + String.valueOf(summary == null ? "" : summary);
        return organDietAiService.generatePlan(ctx);
    }

    private String buildFallbackAiPlan(String organKey, OrganImpactResp.OrganImpactProfile profile, List<NutritionFoodItemResp> foods) {
        List<OrganDetailResp.Suggestion> sug = buildSuggestions(organKey, profile, foods);
        String joined = sug.stream()
                .filter(Objects::nonNull)
                .map(s -> String.valueOf(s.getTitle() == null ? "" : s.getTitle()).trim() + "：" + String.valueOf(s.getDescription() == null ? "" : s.getDescription()).trim())
                .filter(s -> !s.equals("："))
                .limit(3)
                .collect(Collectors.joining("；"));
        return joined.isEmpty() ? "" : joined;
    }

    private List<Integer> buildHistory7d(Long userId, Long memberId, String organKey, String dateStr) {
        LocalDate base = parseDateOrToday(dateStr);
        List<Integer> out = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = base.minusDays(i);
            String ds = d.toString();
            OrganImpactResp r = getOrganImpact(userId, memberId, ds);
            OrganImpactResp.OrganImpactItem impact = r == null || r.getImpactByKey() == null ? null : r.getImpactByKey().get(organKey);
            int score = clampScore(baseScore(organKey) + (impact == null ? 0 : impact.getDelta()));
            out.add(score);
        }
        return out;
    }

    private List<OrganDetailResp.OrganTab> buildTabs(Long userId, Long memberId, String dateStr) {
        OrganImpactResp r = getOrganImpact(userId, memberId, dateStr);
        Map<String, OrganImpactResp.OrganImpactItem> map = r == null ? Map.of() : (r.getImpactByKey() == null ? Map.of() : r.getImpactByKey());
        List<String> keys = List.of("heart", "liver", "kidney", "brain", "pancreas", "intestine", "stomach", "lungs", "eyes");
        List<OrganDetailResp.OrganTab> out = new ArrayList<>();
        for (String k : keys) {
            OrganMeta meta = organMeta(k);
            OrganImpactResp.OrganImpactItem it = map.get(k);
            int score = clampScore(baseScore(k) + (it == null ? 0 : it.getDelta()));
            out.add(new OrganDetailResp.OrganTab(meta.key(), meta.name(), meta.emoji(), score));
        }
        out.sort(Comparator.comparingInt(OrganDetailResp.OrganTab::getScore).reversed());
        return out;
    }

    private LocalDate parseDateOrToday(String dateStr) {
        String s = String.valueOf(dateStr == null ? "" : dateStr).trim();
        if (s.isEmpty()) return LocalDate.now();
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
