package com.xs.ai.aiheathagent.vision;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.controller.BaseController;
import com.xs.ai.aiheathagent.dto.req.NutritionFoodItemReq;
import com.xs.ai.aiheathagent.dto.resp.FoodCatalogItemResp;
import com.xs.ai.aiheathagent.entity.NutritionMealType;
import com.xs.ai.aiheathagent.entity.NutritionRecordSource;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.FoodCatalogService;
import com.xs.ai.aiheathagent.service.NutritionService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class MealRecognitionController extends BaseController {
    private final FoodVisionService foodVisionService;
    private final FoodCatalogService foodCatalogService;
    private final MealRecognitionAiService mealRecognitionAiService;
    private final NutritionService nutritionService;

    public MealRecognitionController(
            FoodVisionService foodVisionService,
            FoodCatalogService foodCatalogService,
            MealRecognitionAiService mealRecognitionAiService,
            NutritionService nutritionService,
            UserRepo userRepo
    ) {
        super(userRepo);
        this.foodVisionService = foodVisionService;
        this.foodCatalogService = foodCatalogService;
        this.mealRecognitionAiService = mealRecognitionAiService;
        this.nutritionService = nutritionService;
    }

    @PostMapping(
            value = {"/api/recognition/meal/recognize", "/api/recognition/meal/recognize/", "/api/api/recognition/meal/recognize", "/api/api/recognition/meal/recognize/", "/recognition/meal/recognize", "/recognition/meal/recognize/"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResp<MealRecognizeResp> recognize(@NotNull @RequestPart("file") MultipartFile file) throws Exception {
        var user = current();
        List<FoodCandidate> candidates = foodVisionService.recognizeDish(file);
        candidates.sort(Comparator.comparingDouble((FoodCandidate c) -> safeProbability(c)).reversed());
        enrichTopWithMacros(candidates);

        FoodCandidate top1 = candidates.isEmpty() ? null : candidates.get(0);
        MealRecognitionAiService.MealRecognitionAiInput in = new MealRecognitionAiService.MealRecognitionAiInput();
        in.setTop1Name(String.valueOf(top1 == null ? "" : top1.getName()).trim());
        in.setTop1Probability(safeD(top1 == null ? null : top1.getProbability()));
        in.setTop1Calorie(safeD(top1 == null ? null : top1.getCalorie()));
        in.setTop1Carb(safeD(top1 == null ? null : top1.getCarb()));
        in.setTop1Protein(safeD(top1 == null ? null : top1.getProtein()));
        in.setTop1Fat(safeD(top1 == null ? null : top1.getFat()));
        in.setCandidates(candidates);

        MealRecognitionAiService.MealRecognitionAiOutput ai = mealRecognitionAiService.analyze(in);

        MealRecognizeResp out = new MealRecognizeResp();
        out.setUserId(user.getId());
        out.setTop1Name(in.getTop1Name());
        out.setTop1Probability(in.getTop1Probability());
        out.setSummary(ai == null ? "" : String.valueOf(ai.getSummary()));
        out.setNutritionText(ai == null ? "" : String.valueOf(ai.getNutritionText()));
        out.setNotesText(ai == null ? "" : String.valueOf(ai.getNotesText()));
        out.setImpactText(ai == null ? "" : String.valueOf(ai.getImpactText()));

        List<MealRecognitionAiService.MealItem> items = ai == null ? List.of() : (ai.getItems() == null ? List.of() : ai.getItems());
        List<MealItemResp> uiItems = new ArrayList<>();
        for (MealRecognitionAiService.MealItem it : items) {
            if (it == null) continue;
            String name = String.valueOf(it.getName() == null ? "" : it.getName()).trim();
            if (name.isEmpty()) continue;
            MealItemResp m = new MealItemResp();
            m.setName(name);
            m.setPortion(String.valueOf(it.getPortion() == null ? "" : it.getPortion()).trim());
            m.setCalories(it.getCalories() == null ? 0 : Math.max(0, it.getCalories()));
            uiItems.add(m);
        }
        if (uiItems.isEmpty()) {
            uiItems = fallbackUiItems(candidates);
        }
        out.setItems(uiItems);

        List<RecordFood> recordFoods = buildRecordFoods(candidates);
        out.setRecordFoods(recordFoods);
        return ApiResp.ok(out);
    }

    @PostMapping(
            value = {"/api/recognition/meal/add-to-brochure", "/api/recognition/meal/add-to-brochure/", "/api/api/recognition/meal/add-to-brochure", "/api/api/recognition/meal/add-to-brochure/", "/recognition/meal/add-to-brochure", "/recognition/meal/add-to-brochure/"},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResp<Map<String, Object>> addToBrochure(@RequestBody(required = false) AddToBrochureReq req) {
        var user = current();
        AddToBrochureReq r = req == null ? new AddToBrochureReq() : req;

        Long memberId = r.getMemberId();
        if (memberId == null || memberId <= 0) {
            return ApiResp.fail(400, "memberId required");
        }

        String mealTypeRaw = String.valueOf(r.getMealType() == null ? "" : r.getMealType()).trim();
        NutritionMealType mealType;
        try {
            mealType = NutritionMealType.valueOf(mealTypeRaw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ApiResp.fail(400, "invalid mealType");
        }
        if (mealType == NutritionMealType.SNACK) {
            return ApiResp.fail(400, "mealType cannot be SNACK");
        }

        String date = String.valueOf(r.getDate() == null ? "" : r.getDate()).trim();
        if (date.isEmpty()) {
            date = LocalDate.now().toString();
        }
        List<NutritionFoodItemReq> foods = new ArrayList<>();
        List<RecordFood> incoming = r.getFoods() == null ? List.of() : r.getFoods();
        for (RecordFood it : incoming) {
            if (it == null) continue;
            NutritionFoodItemReq f = new NutritionFoodItemReq();
            f.setName(String.valueOf(it.getName() == null ? "" : it.getName()).trim());
            f.setUnit(String.valueOf(it.getUnit() == null ? "" : it.getUnit()).trim());
            f.setNutrient(String.valueOf(it.getNutrient() == null ? "" : it.getNutrient()).trim());
            f.setImg(String.valueOf(it.getImg() == null ? "" : it.getImg()).trim());
            f.setNote(String.valueOf(it.getNote() == null ? "" : it.getNote()).trim());
            f.setCalorie(it.getCalorie() == null ? 0 : Math.max(0, it.getCalorie()));
            f.setCarb(it.getCarb() == null ? 0.0 : it.getCarb());
            f.setProtein(it.getProtein() == null ? 0.0 : it.getProtein());
            f.setFat(it.getFat() == null ? 0.0 : it.getFat());
            if (f.getName() == null || f.getName().isBlank()) continue;
            foods.add(f);
            if (foods.size() >= 8) break;
        }
        if (foods.isEmpty()) {
            return ApiResp.fail(400, "foods required");
        }

        int added = nutritionService.appendFoods(user.getId(), memberId, date, mealType, foods, NutritionRecordSource.VISION);
        return ApiResp.ok(Map.of(
                "ok", true,
                "added", added,
                "date", date,
                "mealType", mealType.name()
        ));
    }

    private List<RecordFood> buildRecordFoods(List<FoodCandidate> candidates) {
        List<FoodCandidate> list = candidates == null ? List.of() : candidates;
        List<RecordFood> out = new ArrayList<>();
        for (FoodCandidate c : list) {
            if (c == null) continue;
            String name = String.valueOf(c.getName() == null ? "" : c.getName()).trim();
            if (name.isEmpty()) continue;
            RecordFood f = new RecordFood();
            f.setName(name);
            f.setUnit("份");
            f.setCalorie(Math.max(0, (int) Math.round(safeD(c.getCalorie()))));
            f.setCarb(safeD(c.getCarb()));
            f.setProtein(safeD(c.getProtein()));
            f.setFat(safeD(c.getFat()));
            f.setNutrient("");
            f.setImg("");
            f.setNote("AI识别");
            out.add(f);
            if (out.size() >= 3) break;
        }
        return out;
    }

    private List<MealItemResp> fallbackUiItems(List<FoodCandidate> candidates) {
        List<FoodCandidate> list = candidates == null ? List.of() : candidates;
        List<MealItemResp> out = new ArrayList<>();
        for (FoodCandidate c : list) {
            if (c == null) continue;
            String name = String.valueOf(c.getName() == null ? "" : c.getName()).trim();
            if (name.isEmpty()) continue;
            MealItemResp m = new MealItemResp();
            m.setName(name);
            m.setPortion("一份");
            m.setCalories(Math.max(0, (int) Math.round(safeD(c.getCalorie()))));
            out.add(m);
            if (out.size() >= 3) break;
        }
        return out;
    }

    private void enrichTopWithMacros(List<FoodCandidate> items) {
        if (items == null || items.isEmpty()) return;
        items.sort(Comparator.comparingDouble((FoodCandidate c) -> safeProbability(c)).reversed());
        FoodCandidate top = items.get(0);
        String name = String.valueOf(top == null ? "" : top.getName()).trim();
        if (name.isEmpty()) return;
        List<FoodCatalogItemResp> list = foodCatalogService.search(name, 8);
        FoodCatalogItemResp best = pickBest(name, list);
        if (best == null) return;
        if (top.getCarb() == null) top.setCarb(best.getCarb());
        if (top.getProtein() == null) top.setProtein(best.getProtein());
        if (top.getFat() == null) top.setFat(best.getFat());
        if (top.getCalorie() == null || top.getCalorie() <= 0) top.setCalorie((double) best.getCalorie());
    }

    private double safeProbability(FoodCandidate c) {
        if (c == null || c.getProbability() == null || c.getProbability().isNaN()) return 0.0;
        return Math.max(0.0, Math.min(1.0, c.getProbability()));
    }

    private double safeD(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) return 0.0;
        return v;
    }

    private FoodCatalogItemResp pickBest(String q, List<FoodCatalogItemResp> list) {
        String query = normName(q);
        if (query.isEmpty() || list == null || list.isEmpty()) return null;
        FoodCatalogItemResp best = null;
        int bestScore = -1;
        for (FoodCatalogItemResp it : list) {
            if (it == null) continue;
            String name = String.valueOf(it.getName() == null ? "" : it.getName()).trim();
            if (name.isEmpty()) continue;
            String n = normName(name);
            int score = matchScore(query, n);
            if (score > bestScore) {
                bestScore = score;
                best = it;
            }
        }
        return bestScore <= 0 ? null : best;
    }

    private String normName(String s) {
        String t = String.valueOf(s == null ? "" : s).trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return "";
        return t.replaceAll("[\\s()（）\\[\\]【】,，.。·•\\-_/]+", "");
    }

    private int matchScore(String query, String candidate) {
        if (query.isEmpty() || candidate.isEmpty()) return 0;
        if (candidate.equals(query)) return 4;
        if (candidate.startsWith(query) || query.startsWith(candidate)) return 3;
        if (candidate.contains(query) || query.contains(candidate)) return 2;
        return 1;
    }

    @Data
    public static class MealRecognizeResp {
        private Long userId;
        private String top1Name;
        private Double top1Probability;
        private String summary;
        private String nutritionText;
        private String notesText;
        private String impactText;
        private List<MealItemResp> items;
        private List<RecordFood> recordFoods;
    }

    @Data
    public static class MealItemResp {
        private String name;
        private String portion;
        private Integer calories;
    }

    @Data
    public static class AddToBrochureReq {
        private Long memberId;
        private String date;
        private String mealType;
        private List<RecordFood> foods;
    }

    @Data
    public static class RecordFood {
        private String name;
        private String unit;
        private String nutrient;
        private Integer calorie;
        private Double carb;
        private Double protein;
        private Double fat;
        private String img;
        private String note;
    }
}
