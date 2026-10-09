package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.dto.resp.OrganImpactResp;
import com.xs.ai.aiheathagent.dto.resp.RecipeCardResp;
import com.xs.ai.aiheathagent.dto.resp.RecipeDetailResp;
import com.xs.ai.aiheathagent.dto.resp.RecipeRecommendResp;
import com.xs.ai.aiheathagent.entity.Recipe;
import com.xs.ai.aiheathagent.entity.RecipeIngredient;
import com.xs.ai.aiheathagent.entity.RecipeStep;
import com.xs.ai.aiheathagent.repository.RecipeRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final OrganImpactService organImpactService;

    public RecipeService(RecipeRepository recipeRepository, OrganImpactService organImpactService) {
        this.recipeRepository = recipeRepository;
        this.organImpactService = organImpactService;
    }

    @PostConstruct
    @Transactional
    public void seedBuiltinRecipes() {
        seedIfMissing(recipe1());
        seedIfMissing(recipe2());
        seedIfMissing(recipe3());
        seedIfMissing(recipe4());
        seedIfMissing(recipe5());
        seedIfMissing(recipe6());
    }

    private void seedIfMissing(Recipe r) {
        if (r == null) return;
        String title = String.valueOf(r.getTitle() == null ? "" : r.getTitle()).trim();
        if (title.isEmpty()) return;
        boolean exists = recipeRepository.findTopByTitleIgnoreCase(title).isPresent();
        if (exists) return;
        recipeRepository.save(r);
    }

    @Transactional
    public RecipeRecommendResp recommend(Long userId, Long memberId, String date, Integer limit) {
        int size = Math.min(Math.max(1, limit == null ? 6 : limit), 20);
        OrganImpactResp impact = organImpactService.getOrganImpact(userId, memberId, date);
        String dt = String.valueOf(impact == null ? "" : impact.getDate());
        if (dt.isBlank()) dt = String.valueOf(date == null ? "" : date);

        List<String> focus = pickFocusOrgans(impact);
        OrganImpactResp.OrganImpactProfile profile = impact == null ? null : impact.getProfile();

        List<Recipe> candidates = recipeRepository.listActive(PageRequest.of(0, 50));
        List<Recipe> ranked = rank(candidates, focus, profile);
        List<RecipeCardResp> out = ranked.stream()
                .limit(size)
                .map((r) -> toCard(r, focus, profile))
                .toList();

        String tips = buildTips(focus, profile);
        return new RecipeRecommendResp(memberId, dt, tips, focus, out);
    }

    @Transactional
    public RecipeDetailResp detail(Long userId, Long memberId, String date, Long recipeId) {
        Recipe r = recipeRepository.findById(recipeId).orElse(null);
        if (r == null || r.getIsActive() == null || r.getIsActive() != 1) return null;

        OrganImpactResp impact = (memberId != null && memberId > 0) ? organImpactService.getOrganImpact(userId, memberId, date) : null;
        List<String> focus = pickFocusOrgans(impact);
        OrganImpactResp.OrganImpactProfile profile = impact == null ? null : impact.getProfile();

        String reason = buildReason(r, focus, profile);
        String nutritionValue = buildNutritionValue(r, reason, focus, profile);

        List<RecipeDetailResp.Ingredient> ingredients = (r.getIngredients() == null ? List.<RecipeIngredient>of() : r.getIngredients())
                .stream()
                .map((it) -> new RecipeDetailResp.Ingredient(
                        String.valueOf(it == null ? "" : it.getName()),
                        String.valueOf(it == null ? "" : it.getAmount())
                ))
                .toList();

        List<RecipeDetailResp.Step> steps = (r.getSteps() == null ? List.<RecipeStep>of() : r.getSteps())
                .stream()
                .map((s) -> new RecipeDetailResp.Step(
                        String.valueOf(s == null ? "" : s.getTitle()),
                        String.valueOf(s == null ? "" : s.getDescription()),
                        s == null || s.getTimeMinutes() == null ? 0 : Math.max(0, s.getTimeMinutes())
                ))
                .toList();

        return new RecipeDetailResp(
                r.getId(),
                String.valueOf(r.getTitle()),
                safeInt(r.getCalories()),
                safeInt(r.getTimeMinutes()),
                reason,
                String.valueOf(r.getImageUrl()),
                safeDouble(r.getCarbG()),
                safeDouble(r.getProteinG()),
                safeDouble(r.getFatG()),
                safeDouble(r.getFiberG()),
                ingredients,
                steps,
                nutritionValue
        );
    }

    private List<String> pickFocusOrgans(OrganImpactResp impact) {
        List<OrganImpactResp.OrganImpactItem> impacts = impact == null || impact.getImpacts() == null ? List.of() : impact.getImpacts();
        if (impacts.isEmpty()) return List.of("intestine", "pancreas");
        List<OrganImpactResp.OrganImpactItem> copy = new ArrayList<>(impacts);
        copy.sort(Comparator.comparingInt((OrganImpactResp.OrganImpactItem it) -> it == null ? 0 : it.getDelta()).thenComparing((it) -> String.valueOf(it == null ? "" : it.getKey())));
        List<String> focus = new ArrayList<>();
        for (OrganImpactResp.OrganImpactItem it : copy) {
            String k = String.valueOf(it == null ? "" : it.getKey()).trim();
            if (k.isEmpty()) continue;
            if (focus.contains(k)) continue;
            focus.add(k);
            if (focus.size() >= 2) break;
        }
        if (focus.isEmpty()) return List.of("intestine", "pancreas");
        return focus;
    }

    private List<Recipe> rank(List<Recipe> candidates, List<String> focusOrgans, OrganImpactResp.OrganImpactProfile profile) {
        List<Recipe> list = candidates == null ? List.of() : candidates;
        return list.stream()
                .sorted(Comparator
                        .comparingInt((Recipe r) -> score(r, focusOrgans, profile)).reversed()
                        .thenComparing((Recipe r) -> safeInt(r == null ? null : r.getSortOrder()), Comparator.reverseOrder())
                        .thenComparing((Recipe r) -> safeLong(r == null ? null : r.getId())))
                .collect(Collectors.toList());
    }

    private int score(Recipe r, List<String> focusOrgans, OrganImpactResp.OrganImpactProfile profile) {
        if (r == null) return -9999;
        int s = safeInt(r.getSortOrder());
        Set<String> organs = splitSet(r.getOrganKeys());
        Set<String> tags = splitSet(r.getTags());
        for (String ok : (focusOrgans == null ? List.<String>of() : focusOrgans)) {
            if (ok != null && organs.contains(ok)) s += 10;
        }

        int carbPct = safeInt(profile == null ? null : (profile.getMacroPercent() == null ? null : profile.getMacroPercent().get("carb")));
        int sugarHits = safeInt(profile == null ? null : profile.getSugarHits());
        int saltyHits = safeInt(profile == null ? null : profile.getSaltyHits());
        int omega3 = safeInt(profile == null ? null : profile.getOmega3Score());
        double fiber = safeDouble(profile == null ? null : profile.getFiberG());

        if ((sugarHits > 0 || carbPct > 120) && (tags.contains("low_carb") || tags.contains("low_sugar"))) s += 8;
        if (fiber > 0 && fiber < 15 && tags.contains("high_fiber")) s += 8;
        if (saltyHits > 0 && tags.contains("low_salt")) s += 8;
        if (omega3 > 0 && omega3 < 3 && tags.contains("omega3")) s += 8;

        return s;
    }

    private RecipeCardResp toCard(Recipe r, List<String> focus, OrganImpactResp.OrganImpactProfile profile) {
        if (r == null) return null;
        return new RecipeCardResp(
                r.getId(),
                String.valueOf(r.getTitle()),
                safeInt(r.getCalories()),
                safeInt(r.getTimeMinutes()),
                buildReason(r, focus, profile),
                String.valueOf(r.getImageUrl())
        );
    }

    private String buildTips(List<String> focusOrgans, OrganImpactResp.OrganImpactProfile profile) {
        List<String> organs = focusOrgans == null ? List.of() : focusOrgans;
        List<String> names = organs.stream().map(this::organName).filter((s) -> s != null && !s.isBlank()).toList();
        String organPart = names.isEmpty() ? "人体图" : ("人体图里更需要关注的部位（" + String.join("、", names) + "）");

        int carbPct = safeInt(profile == null ? null : (profile.getMacroPercent() == null ? null : profile.getMacroPercent().get("carb")));
        int sugarHits = safeInt(profile == null ? null : profile.getSugarHits());
        int saltyHits = safeInt(profile == null ? null : profile.getSaltyHits());
        int omega3 = safeInt(profile == null ? null : profile.getOmega3Score());
        double fiber = safeDouble(profile == null ? null : profile.getFiberG());

        List<String> reasons = new ArrayList<>();
        if (sugarHits > 0 || carbPct > 120) reasons.add("控糖/稳碳水");
        if (fiber > 0 && fiber < 15) reasons.add("补纤维");
        if (saltyHits > 0) reasons.add("控盐");
        if (omega3 > 0 && omega3 < 3) reasons.add("补 Omega-3");

        String why = reasons.isEmpty() ? "更适合你今天的能量与营养结构" : ("优先考虑" + String.join("、", reasons) + "的方向");
        return "我们会综合你今天的营养册记录与" + organPart + "，" + why + "来推荐菜谱。";
    }

    private String buildReason(Recipe r, List<String> focusOrgans, OrganImpactResp.OrganImpactProfile profile) {
        if (r == null) return "";
        Set<String> tags = splitSet(r.getTags());
        Set<String> organs = splitSet(r.getOrganKeys());
        int carbPct = safeInt(profile == null ? null : (profile.getMacroPercent() == null ? null : profile.getMacroPercent().get("carb")));
        int sugarHits = safeInt(profile == null ? null : profile.getSugarHits());
        int saltyHits = safeInt(profile == null ? null : profile.getSaltyHits());
        int omega3 = safeInt(profile == null ? null : profile.getOmega3Score());
        double fiber = safeDouble(profile == null ? null : profile.getFiberG());

        if ((sugarHits > 0 || carbPct > 120) && (tags.contains("low_carb") || tags.contains("low_sugar") || organs.contains("pancreas"))) {
            return "你今天碳水/甜食偏多，这道菜用更高蛋白与蔬菜来稳住波动，对胰腺更友好。";
        }
        if (fiber > 0 && fiber < 15 && (tags.contains("high_fiber") || organs.contains("intestine"))) {
            return "你今天纤维偏少，这道菜蔬菜/全谷更足，支持肠道状态与饱腹感。";
        }
        if (saltyHits > 0 && (tags.contains("low_salt") || organs.contains("heart") || organs.contains("kidney"))) {
            return "你今天盐分需要关注，这道菜更清淡，减轻心脏与肾的负担。";
        }
        if (omega3 > 0 && omega3 < 3 && (tags.contains("omega3") || organs.contains("brain") || organs.contains("eyes"))) {
            return "你今天 Omega-3 需要补一点，这道菜用深海鱼/坚果支持心脑与视力。";
        }
        for (String ok : (focusOrgans == null ? List.<String>of() : focusOrgans)) {
            if (ok != null && organs.contains(ok)) {
                String name = organName(ok);
                if (name != null && !name.isBlank()) return "针对你今天更需要关注的" + name + "，这道菜的搭配更友好。";
            }
        }
        String fallback = String.valueOf(r.getDefaultReason() == null ? "" : r.getDefaultReason()).trim();
        if (!fallback.isEmpty()) return fallback;
        return "这道菜更适合你今天的营养结构。";
    }

    private String buildNutritionValue(Recipe r, String reason, List<String> focusOrgans, OrganImpactResp.OrganImpactProfile profile) {
        String base = String.valueOf(reason == null ? "" : reason).trim();
        String stored = String.valueOf(r == null || r.getValueText() == null ? "" : r.getValueText()).trim();
        if (!stored.isEmpty()) return stored;

        double carb = safeDouble(r == null ? null : r.getCarbG());
        double protein = safeDouble(r == null ? null : r.getProteinG());
        double fat = safeDouble(r == null ? null : r.getFatG());
        double fiber = safeDouble(r == null ? null : r.getFiberG());

        List<String> points = new ArrayList<>();
        if (protein > 0) points.add("蛋白质约 " + round1(protein) + "g");
        if (carb > 0) points.add("碳水约 " + round1(carb) + "g");
        if (fat > 0) points.add("脂肪约 " + round1(fat) + "g");
        if (fiber > 0) points.add("纤维约 " + round1(fiber) + "g");

        String macro = points.isEmpty() ? "" : ("每份主要营养：" + String.join("，", points) + "。");
        String organ = (focusOrgans == null || focusOrgans.isEmpty()) ? "" : ("同时更照顾" + focusOrgans.stream().map(this::organName).filter((s) -> s != null && !s.isBlank()).collect(Collectors.joining("、")) + "相关的饮食方向。");

        String suffix = macro + organ;
        if (base.isEmpty()) return suffix;
        if (suffix.isEmpty()) return base;
        return base + " " + suffix;
    }

    private Set<String> splitSet(String raw) {
        String s = String.valueOf(raw == null ? "" : raw).trim();
        if (s.isEmpty()) return Set.of();
        String[] parts = s.split("[,，\\s]+");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            String v = String.valueOf(p == null ? "" : p).trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty()) continue;
            out.add(v);
        }
        return out;
    }

    private String organName(String organKey) {
        return switch (String.valueOf(organKey == null ? "" : organKey).trim()) {
            case "heart" -> "心脏";
            case "brain" -> "大脑";
            case "lungs" -> "肺";
            case "liver" -> "肝";
            case "stomach" -> "胃";
            case "intestine" -> "肠道";
            case "pancreas" -> "胰腺";
            case "kidney" -> "肾";
            case "eyes" -> "眼睛";
            default -> "";
        };
    }

    private int safeInt(Integer v) { return v == null ? 0 : v; }
    private long safeLong(Long v) { return v == null ? 0L : v; }
    private double safeDouble(Double v) { return v == null ? 0.0 : v; }
    private String round1(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    private Recipe recipe1() {
        Recipe r = new Recipe();
        r.setTitle("鸡胸肉水果蔬菜沙拉");
        r.setCalories(320);
        r.setTimeMinutes(15);
        r.setCarbG(18.0);
        r.setProteinG(35.0);
        r.setFatG(12.0);
        r.setFiberG(8.0);
        r.setDefaultReason("高蛋白搭配足量蔬菜，饱腹感更强，热量更可控。");
        r.setImageUrl("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=800&q=80");
        r.setOrganKeys("intestine,pancreas,heart");
        r.setTags("high_protein,high_fiber,low_carb,low_salt");
        r.setSortOrder(90);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setIngredients(List.of(
                new RecipeIngredient("生菜", "200g"),
                new RecipeIngredient("鸡胸肉", "120g"),
                new RecipeIngredient("小番茄", "80g"),
                new RecipeIngredient("黄瓜", "60g"),
                new RecipeIngredient("胡萝卜", "30g"),
                new RecipeIngredient("橄榄油", "10ml"),
                new RecipeIngredient("柠檬汁", "10ml")
        ));
        r.setSteps(List.of(
                new RecipeStep("准备食材", "鸡胸肉切块，少量盐和黑胡椒腌制。蔬菜洗净切好。", 10),
                new RecipeStep("煎鸡胸肉", "平底锅少油，鸡胸肉煎至两面金黄熟透。", 6),
                new RecipeStep("混合装盘", "将蔬菜与鸡胸肉混合，淋橄榄油与柠檬汁拌匀。", 3)
        ));
        return r;
    }

    private Recipe recipe2() {
        Recipe r = new Recipe();
        r.setTitle("藜麦水果坚果能量碗");
        r.setCalories(285);
        r.setTimeMinutes(10);
        r.setCarbG(35.0);
        r.setProteinG(12.0);
        r.setFatG(10.0);
        r.setFiberG(6.0);
        r.setDefaultReason("慢释碳水搭配健康脂肪，能量更稳定。");
        r.setImageUrl("https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=800&q=80");
        r.setOrganKeys("pancreas,brain");
        r.setTags("steady_carb,omega3,high_fiber");
        r.setSortOrder(80);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setIngredients(List.of(
                new RecipeIngredient("熟藜麦", "120g"),
                new RecipeIngredient("香蕉", "1根"),
                new RecipeIngredient("蓝莓", "50g"),
                new RecipeIngredient("核桃", "15g"),
                new RecipeIngredient("无糖酸奶", "80g")
        ));
        r.setSteps(List.of(
                new RecipeStep("装碗", "把熟藜麦铺底。", 2),
                new RecipeStep("加水果", "香蕉切片，加入蓝莓。", 3),
                new RecipeStep("加坚果酸奶", "加入核桃与无糖酸奶，拌匀或分区摆放。", 3)
        ));
        return r;
    }

    private Recipe recipe3() {
        Recipe r = new Recipe();
        r.setTitle("烤三文鱼蔬菜拼盘");
        r.setCalories(380);
        r.setTimeMinutes(20);
        r.setCarbG(18.0);
        r.setProteinG(32.0);
        r.setFatG(20.0);
        r.setFiberG(7.0);
        r.setDefaultReason("优质蛋白与 Omega-3，支持恢复与抗炎。");
        r.setImageUrl("https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=800&q=80");
        r.setOrganKeys("heart,brain,eyes");
        r.setTags("high_protein,omega3,low_carb,low_salt");
        r.setSortOrder(85);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setIngredients(List.of(
                new RecipeIngredient("三文鱼", "150g"),
                new RecipeIngredient("西兰花", "200g"),
                new RecipeIngredient("彩椒", "80g"),
                new RecipeIngredient("橄榄油", "10ml"),
                new RecipeIngredient("黑胡椒", "适量"),
                new RecipeIngredient("柠檬", "半个")
        ));
        r.setSteps(List.of(
                new RecipeStep("预热与准备", "烤箱预热 200℃，三文鱼抹少量橄榄油与黑胡椒。", 5),
                new RecipeStep("烤制", "三文鱼与蔬菜同盘烤制，至熟透。", 12),
                new RecipeStep("调味", "出炉挤柠檬汁，按口味少量盐或不加盐。", 2)
        ));
        return r;
    }

    private Recipe recipe4() {
        Recipe r = new Recipe();
        r.setTitle("绿色能量果昔碗");
        r.setCalories(220);
        r.setTimeMinutes(5);
        r.setCarbG(28.0);
        r.setProteinG(10.0);
        r.setFatG(6.0);
        r.setFiberG(9.0);
        r.setDefaultReason("低热量高纤维，作为加餐更轻盈。");
        r.setImageUrl("https://images.unsplash.com/photo-1478145046317-39f10e56b5e9?auto=format&fit=crop&w=800&q=80");
        r.setOrganKeys("intestine,liver");
        r.setTags("high_fiber,low_calorie,low_sugar");
        r.setSortOrder(70);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setIngredients(List.of(
                new RecipeIngredient("菠菜", "50g"),
                new RecipeIngredient("香蕉", "1根"),
                new RecipeIngredient("牛油果", "半个"),
                new RecipeIngredient("无糖豆浆", "200ml"),
                new RecipeIngredient("奇亚籽", "10g")
        ));
        r.setSteps(List.of(
                new RecipeStep("打碎", "将菠菜、香蕉、牛油果与无糖豆浆打至顺滑。", 3),
                new RecipeStep("装碗点缀", "倒入碗中，撒奇亚籽或少量坚果。", 2)
        ));
        return r;
    }

    private Recipe recipe5() {
        Recipe r = new Recipe();
        r.setTitle("番茄鸡蛋虾仁汤");
        r.setCalories(260);
        r.setTimeMinutes(15);
        r.setCarbG(10.0);
        r.setProteinG(24.0);
        r.setFatG(12.0);
        r.setFiberG(3.0);
        r.setDefaultReason("清淡易消化，补蛋白同时控制盐分。");
        r.setImageUrl("https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=800&q=80");
        r.setOrganKeys("stomach,heart,kidney");
        r.setTags("low_salt,high_protein");
        r.setSortOrder(60);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setIngredients(List.of(
                new RecipeIngredient("番茄", "2个"),
                new RecipeIngredient("鸡蛋", "2个"),
                new RecipeIngredient("虾仁", "120g"),
                new RecipeIngredient("葱花", "少量"),
                new RecipeIngredient("盐", "少量或不放")
        ));
        r.setSteps(List.of(
                new RecipeStep("炒番茄", "番茄切块，小火炒出汁。", 4),
                new RecipeStep("加水煮", "加水煮开后下虾仁至变色。", 6),
                new RecipeStep("打蛋花", "慢慢倒入蛋液形成蛋花，少量盐或不放。", 3)
        ));
        return r;
    }

    private Recipe recipe6() {
        Recipe r = new Recipe();
        r.setTitle("燕麦酸奶莓果杯");
        r.setCalories(240);
        r.setTimeMinutes(8);
        r.setCarbG(30.0);
        r.setProteinG(14.0);
        r.setFatG(7.0);
        r.setFiberG(7.0);
        r.setDefaultReason("纤维+蛋白组合，稳住饥饿感与血糖波动。");
        r.setImageUrl("https://images.unsplash.com/photo-1514996937319-344454492b37?auto=format&fit=crop&w=800&q=80");
        r.setOrganKeys("pancreas,intestine");
        r.setTags("high_fiber,low_sugar,steady_carb");
        r.setSortOrder(65);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r.setIngredients(List.of(
                new RecipeIngredient("燕麦片", "40g"),
                new RecipeIngredient("无糖希腊酸奶", "150g"),
                new RecipeIngredient("蓝莓", "60g"),
                new RecipeIngredient("坚果碎", "10g")
        ));
        r.setSteps(List.of(
                new RecipeStep("分层装杯", "杯底铺燕麦，再加酸奶。", 3),
                new RecipeStep("加莓果坚果", "加蓝莓与坚果碎，冷藏或即食。", 3)
        ));
        return r;
    }
}

