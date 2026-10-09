package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.xs.ai.aiheathagent.dto.req.NutritionDaySaveReq;
import com.xs.ai.aiheathagent.dto.req.NutritionFoodItemReq;
import com.xs.ai.aiheathagent.dto.req.NutritionMemberUpsertReq;
import com.xs.ai.aiheathagent.dto.req.NutritionSnackCardReq;
import com.xs.ai.aiheathagent.dto.resp.NutritionDayResp;
import com.xs.ai.aiheathagent.dto.resp.NutritionFoodItemResp;
import com.xs.ai.aiheathagent.dto.resp.NutritionMemberResp;
import com.xs.ai.aiheathagent.dto.resp.NutritionSnackCardResp;
import com.xs.ai.aiheathagent.entity.FamilyMember;
import com.xs.ai.aiheathagent.entity.NutritionMealRecord;
import com.xs.ai.aiheathagent.entity.NutritionMealType;
import com.xs.ai.aiheathagent.entity.NutritionRecordSource;
import com.xs.ai.aiheathagent.entity.NutritionSnackCard;
import com.xs.ai.aiheathagent.repository.FamilyMemberRepository;
import com.xs.ai.aiheathagent.repository.NutritionMealRecordRepository;
import com.xs.ai.aiheathagent.repository.NutritionSnackCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NutritionService {
    private static final Pattern MACRO_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*克?\\s*(碳水|蛋白|脂肪)");

    private final FamilyMemberRepository familyMemberRepository;
    private final NutritionMealRecordRepository nutritionMealRecordRepository;
    private final NutritionSnackCardRepository nutritionSnackCardRepository;

    public NutritionService(
            FamilyMemberRepository familyMemberRepository,
            NutritionMealRecordRepository nutritionMealRecordRepository,
            NutritionSnackCardRepository nutritionSnackCardRepository
    ) {
        this.familyMemberRepository = familyMemberRepository;
        this.nutritionMealRecordRepository = nutritionMealRecordRepository;
        this.nutritionSnackCardRepository = nutritionSnackCardRepository;
    }

    @Transactional
    public List<NutritionMemberResp> listMembers(Long userId) {
        ensureSelfMember(userId);
        List<FamilyMember> members = familyMemberRepository.findByUserIdOrderByIdAsc(userId);
        return members.stream().map(this::toMemberResp).collect(Collectors.toList());
    }

    @Transactional
    public NutritionMemberResp createMember(Long userId, NutritionMemberUpsertReq req) {
        String key = String.valueOf(req.getMemberKey() == null ? "" : req.getMemberKey()).trim();
        if (key.isEmpty()) {
            key = "m_" + System.currentTimeMillis();
        }
        String nickname = String.valueOf(req.getNickname() == null ? "" : req.getNickname()).trim();
        String relation = String.valueOf(req.getRelation() == null ? "" : req.getRelation()).trim();
        if (nickname.isEmpty()) throw new BizEx(ErrorCode.PARAM_INVALID, "昵称不能为空");
        if (relation.isEmpty()) throw new BizEx(ErrorCode.PARAM_INVALID, "关系不能为空");

        FamilyMember m = familyMemberRepository.findByUserIdAndMemberKey(userId, key).orElse(null);
        if (m == null) {
            m = new FamilyMember();
            m.setUserId(userId);
            m.setMemberKey(key);
            m.setCreatedAt(LocalDateTime.now());
            m.setCalorieTarget(2000);
            m.setCarbTarget(264.0);
            m.setProteinTarget(100.0);
            m.setFatTarget(67.0);
        }
        m.setNickname(nickname);
        m.setRelation(relation);
        m.setUpdatedAt(LocalDateTime.now());
        familyMemberRepository.save(m);
        return toMemberResp(m);
    }

    @Transactional
    public void deleteMember(Long userId, Long memberId) {
        ensureSelfMember(userId);
        if (memberId == null || memberId <= 0) throw new BizEx(ErrorCode.PARAM_INVALID, "memberId 不存在");
        FamilyMember member = familyMemberRepository.findByIdAndUserId(memberId, userId)
                .orElseThrow(() -> new BizEx(ErrorCode.PARAM_INVALID, "memberId 不存在"));
        if ("me".equals(String.valueOf(member.getMemberKey()))) {
            throw new BizEx(ErrorCode.FORBIDDEN, "不能删除自己");
        }
        nutritionMealRecordRepository.deleteByUserIdAndMemberId(userId, memberId);
        nutritionSnackCardRepository.deleteByUserIdAndMemberId(userId, memberId);
        familyMemberRepository.delete(member);
    }

    @Transactional
    public NutritionDayResp getDay(Long userId, Long memberId, String dateStr) {
        ensureSelfMember(userId);
        FamilyMember member = familyMemberRepository.findByIdAndUserId(memberId, userId)
                .orElseThrow(() -> new BizEx(ErrorCode.PARAM_INVALID, "memberId 不存在"));

        LocalDate day = parseDateOrToday(dateStr);
        List<NutritionMealRecord> records = nutritionMealRecordRepository.findByUserIdAndMemberIdAndRecordDateOrderByIdAsc(userId, member.getId(), day);
        List<NutritionSnackCard> snackCards = nutritionSnackCardRepository.findByUserIdAndMemberIdAndRecordDateOrderByIdAsc(userId, member.getId(), day);
        return buildDayResp(member, day, records, snackCards, null);
    }

    @Transactional
    public NutritionDayResp saveDay(Long userId, NutritionDaySaveReq req) {
        ensureSelfMember(userId);

        Long memberId = req.getMemberId();
        FamilyMember member = familyMemberRepository.findByIdAndUserId(memberId, userId)
                .orElseThrow(() -> new BizEx(ErrorCode.PARAM_INVALID, "memberId 不存在"));

        LocalDate day = parseDateOrToday(req.getDate());

        applyTargetsIfPresent(member, req);
        member.setUpdatedAt(LocalDateTime.now());
        familyMemberRepository.save(member);

        nutritionMealRecordRepository.deleteByUserIdAndMemberIdAndRecordDate(userId, member.getId(), day);
        nutritionSnackCardRepository.deleteByUserIdAndMemberIdAndRecordDate(userId, member.getId(), day);

        List<NutritionSnackCard> savedSnackCards = saveSnackCards(userId, member.getId(), day, req.getSnackCards());
        List<NutritionMealRecord> savedRecords = new ArrayList<>();
        savedRecords.addAll(saveFoods(userId, member.getId(), day, NutritionMealType.BREAKFAST, "", req.getBreakfastFoods()));
        savedRecords.addAll(saveFoods(userId, member.getId(), day, NutritionMealType.LUNCH, "", req.getLunchFoods()));
        savedRecords.addAll(saveFoods(userId, member.getId(), day, NutritionMealType.DINNER, "", req.getDinnerFoods()));
        savedRecords.addAll(saveSnackFoods(userId, member.getId(), day, req.getSnackCards()));

        nutritionMealRecordRepository.saveAll(savedRecords);

        return buildDayResp(member, day, savedRecords, savedSnackCards, req);
    }

    @Transactional
    public int appendFoods(Long userId, Long memberId, String dateStr, NutritionMealType mealType, List<NutritionFoodItemReq> foods, NutritionRecordSource source) {
        ensureSelfMember(userId);
        if (memberId == null || memberId <= 0) throw new BizEx(ErrorCode.PARAM_INVALID, "memberId 不存在");
        if (mealType == null) throw new BizEx(ErrorCode.PARAM_INVALID, "mealType 不能为空");
        if (mealType == NutritionMealType.SNACK) throw new BizEx(ErrorCode.PARAM_INVALID, "mealType 不支持 SNACK");

        FamilyMember member = familyMemberRepository.findByIdAndUserId(memberId, userId)
                .orElseThrow(() -> new BizEx(ErrorCode.PARAM_INVALID, "memberId 不存在"));
        LocalDate day = parseDateOrToday(dateStr);

        List<NutritionMealRecord> out = saveFoods(userId, member.getId(), day, mealType, "", foods);
        NutritionRecordSource src = source == null ? NutritionRecordSource.MANUAL : source;
        for (NutritionMealRecord r : out) {
            if (r == null) continue;
            r.setSource(src);
            r.setUpdatedAt(LocalDateTime.now());
        }
        nutritionMealRecordRepository.saveAll(out);
        return out.size();
    }

    private void ensureSelfMember(Long userId) {
        familyMemberRepository.findByUserIdAndMemberKey(userId, "me").orElseGet(() -> {
            FamilyMember m = new FamilyMember();
            m.setUserId(userId);
            m.setMemberKey("me");
            m.setNickname("我");
            m.setRelation("self");
            m.setCalorieTarget(2000);
            m.setCarbTarget(264.0);
            m.setProteinTarget(100.0);
            m.setFatTarget(67.0);
            m.setCreatedAt(LocalDateTime.now());
            m.setUpdatedAt(LocalDateTime.now());
            return familyMemberRepository.save(m);
        });
    }

    private NutritionMemberResp toMemberResp(FamilyMember m) {
        return new NutritionMemberResp(m.getId(), m.getMemberKey(), m.getNickname(), m.getRelation());
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

    private List<NutritionSnackCard> saveSnackCards(Long userId, Long memberId, LocalDate day, List<NutritionSnackCardReq> cards) {
        List<NutritionSnackCardReq> list = cards == null ? List.of() : cards;
        List<NutritionSnackCard> out = new ArrayList<>();
        for (NutritionSnackCardReq c : list) {
            if (c == null) continue;
            String cardId = String.valueOf(c.getId() == null ? "" : c.getId()).trim();
            if (cardId.isEmpty()) continue;
            int level = c.getLevel() == null ? 0 : Math.max(0, c.getLevel());
            NutritionSnackCard sc = new NutritionSnackCard();
            sc.setUserId(userId);
            sc.setMemberId(memberId);
            sc.setRecordDate(day);
            sc.setCardId(cardId);
            sc.setLevel(level);
            sc.setCreatedAt(LocalDateTime.now());
            sc.setUpdatedAt(LocalDateTime.now());
            out.add(sc);
        }
        return nutritionSnackCardRepository.saveAll(out);
    }

    private List<NutritionMealRecord> saveFoods(Long userId, Long memberId, LocalDate day, NutritionMealType type, String snackCardId, List<NutritionFoodItemReq> foods) {
        List<NutritionFoodItemReq> list = foods == null ? List.of() : foods;
        List<NutritionMealRecord> out = new ArrayList<>();
        for (NutritionFoodItemReq f : list) {
            NutritionMealRecord rec = toRecord(userId, memberId, day, type, snackCardId, f);
            if (rec == null) continue;
            out.add(rec);
        }
        return out;
    }

    private List<NutritionMealRecord> saveSnackFoods(Long userId, Long memberId, LocalDate day, List<NutritionSnackCardReq> snackCards) {
        List<NutritionSnackCardReq> cards = snackCards == null ? List.of() : snackCards;
        List<NutritionMealRecord> out = new ArrayList<>();
        for (NutritionSnackCardReq card : cards) {
            if (card == null) continue;
            String cardId = String.valueOf(card.getId() == null ? "" : card.getId()).trim();
            if (cardId.isEmpty()) continue;
            List<NutritionFoodItemReq> foods = card.getFoods() == null ? List.of() : card.getFoods();
            out.addAll(saveFoods(userId, memberId, day, NutritionMealType.SNACK, cardId, foods));
        }
        return out;
    }

    private NutritionMealRecord toRecord(Long userId, Long memberId, LocalDate day, NutritionMealType type, String snackCardId, NutritionFoodItemReq f) {
        if (f == null) return null;
        String name = String.valueOf(f.getName() == null ? "" : f.getName()).trim();
        if (name.isEmpty()) return null;

        NutritionMealRecord rec = new NutritionMealRecord();
        rec.setUserId(userId);
        rec.setMemberId(memberId);
        rec.setRecordDate(day);
        rec.setMealType(type);
        rec.setFoodName(name);
        rec.setUnit(String.valueOf(f.getUnit() == null ? "" : f.getUnit()).trim());
        rec.setNutrientText(String.valueOf(f.getNutrient() == null ? "" : f.getNutrient()).trim());
        rec.setImageUrl(String.valueOf(f.getImg() == null ? "" : f.getImg()).trim());
        rec.setNote(String.valueOf(f.getNote() == null ? "" : f.getNote()).trim());
        rec.setSnackCardId(String.valueOf(snackCardId == null ? "" : snackCardId).trim());

        int calorie = f.getCalorie() == null ? 0 : Math.max(0, f.getCalorie());
        rec.setCalorie(calorie);

        Macro macro = normalizeMacro(f.getCarb(), f.getProtein(), f.getFat(), rec.getNutrientText());
        rec.setCarb(macro.carb);
        rec.setProtein(macro.protein);
        rec.setFat(macro.fat);
        rec.setFiber(safeMacro(f.getFiber()));

        rec.setSource(guessSource(f));
        rec.setCreatedAt(LocalDateTime.now());
        rec.setUpdatedAt(LocalDateTime.now());
        return rec;
    }

    private double safeMacro(Double v) {
        if (v == null || Double.isNaN(v)) return 0.0;
        return Math.max(0.0, v);
    }

    private NutritionRecordSource guessSource(NutritionFoodItemReq f) {
        String img = String.valueOf(f.getImg() == null ? "" : f.getImg()).trim();
        if (!img.isEmpty() && (img.startsWith("http://") || img.startsWith("https://") || img.startsWith("wxfile://") || img.startsWith("file://") || img.contains("tmp"))) {
            return NutritionRecordSource.MANUAL;
        }
        return NutritionRecordSource.MANUAL;
    }

    private Macro normalizeMacro(Double carb, Double protein, Double fat, String nutrientText) {
        double c = carb == null || Double.isNaN(carb) ? 0.0 : Math.max(0.0, carb);
        double p = protein == null || Double.isNaN(protein) ? 0.0 : Math.max(0.0, protein);
        double ft = fat == null || Double.isNaN(fat) ? 0.0 : Math.max(0.0, fat);
        if (c > 0 || p > 0 || ft > 0) return new Macro(c, p, ft, 0.0);

        String s = String.valueOf(nutrientText == null ? "" : nutrientText);
        Matcher m = MACRO_PATTERN.matcher(s);
        if (!m.find()) return new Macro(0.0, 0.0, 0.0, 0.0);
        double num = safeDouble(m.group(1));
        String type = m.group(2);
        if ("碳水".equals(type)) return new Macro(num, 0.0, 0.0, 0.0);
        if ("蛋白".equals(type)) return new Macro(0.0, num, 0.0, 0.0);
        if ("脂肪".equals(type)) return new Macro(0.0, 0.0, num, 0.0);
        return new Macro(0.0, 0.0, 0.0, 0.0);
    }

    private double safeDouble(String s) {
        try {
            return Double.parseDouble(String.valueOf(s == null ? "" : s).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private NutritionDayResp buildDayResp(FamilyMember member, LocalDate day, List<NutritionMealRecord> records, List<NutritionSnackCard> snackCards, NutritionDaySaveReq req) {
        int calorieTarget = resolveCalorieTarget(member, req);
        double carbTarget = resolveMacroTarget(member == null ? null : member.getCarbTarget(), req == null ? null : req.getCarbTarget(), 264.0);
        double proteinTarget = resolveMacroTarget(member == null ? null : member.getProteinTarget(), req == null ? null : req.getProteinTarget(), 100.0);
        double fatTarget = resolveMacroTarget(member == null ? null : member.getFatTarget(), req == null ? null : req.getFatTarget(), 67.0);

        Map<String, Double> macroTargets = Map.of(
                "carb", carbTarget,
                "protein", proteinTarget,
                "fat", fatTarget
        );

        List<NutritionMealRecord> all = records == null ? List.of() : records;
        List<NutritionFoodItemResp> breakfast = toFoodList(filterMeal(all, NutritionMealType.BREAKFAST));
        List<NutritionFoodItemResp> lunch = toFoodList(filterMeal(all, NutritionMealType.LUNCH));
        List<NutritionFoodItemResp> dinner = toFoodList(filterMeal(all, NutritionMealType.DINNER));

        Map<String, List<NutritionMealRecord>> snackByCard = filterMeal(all, NutritionMealType.SNACK).stream()
                .collect(Collectors.groupingBy(r -> String.valueOf(r.getSnackCardId() == null ? "" : r.getSnackCardId())));

        List<NutritionSnackCard> cards = snackCards == null ? List.of() : snackCards;
        List<NutritionSnackCardResp> snackResp = cards.stream()
                .sorted(Comparator.comparing(NutritionSnackCard::getId, Comparator.nullsLast(Long::compareTo)))
                .map((c) -> new NutritionSnackCardResp(
                        c.getCardId(),
                        c.getLevel() == null ? 0 : c.getLevel(),
                        toFoodList(snackByCard.getOrDefault(c.getCardId(), List.of()))
                ))
                .collect(Collectors.toList());

        List<String> existingCardIds = snackResp.stream().map(NutritionSnackCardResp::getId).filter(Objects::nonNull).toList();
        List<NutritionSnackCardResp> missing = snackByCard.entrySet().stream()
                .filter((e) -> !String.valueOf(e.getKey() == null ? "" : e.getKey()).isEmpty())
                .filter((e) -> !existingCardIds.contains(e.getKey()))
                .map((e) -> new NutritionSnackCardResp(e.getKey(), 0, toFoodList(e.getValue())))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            List<NutritionSnackCardResp> merged = new ArrayList<>(snackResp);
            merged.addAll(missing);
            snackResp = merged;
        }

        Macro totals = sumMacros(all);
        int calorieConsumed = sumCalories(all);
        Map<String, Double> macroTotals = Map.of(
                "carb", round1(totals.carb),
                "protein", round1(totals.protein),
                "fat", round1(totals.fat),
                "fiber", round1(totals.fiber)
        );
        Map<String, Integer> macroPercent = Map.of(
                "carb", clampPct(totals.carb, carbTarget),
                "protein", clampPct(totals.protein, proteinTarget),
                "fat", clampPct(totals.fat, fatTarget)
        );

        return new NutritionDayResp(
                member == null ? null : member.getId(),
                day.toString(),
                calorieConsumed,
                calorieTarget,
                macroTargets,
                macroTotals,
                macroPercent,
                breakfast,
                lunch,
                dinner,
                snackResp
        );
    }

    private void applyTargetsIfPresent(FamilyMember member, NutritionDaySaveReq req) {
        if (member == null || req == null) return;
        if (req.getCalorieTarget() != null) member.setCalorieTarget(Math.max(0, req.getCalorieTarget()));
        if (req.getCarbTarget() != null) member.setCarbTarget(Math.max(0.0, req.getCarbTarget()));
        if (req.getProteinTarget() != null) member.setProteinTarget(Math.max(0.0, req.getProteinTarget()));
        if (req.getFatTarget() != null) member.setFatTarget(Math.max(0.0, req.getFatTarget()));
    }

    private int resolveCalorieTarget(FamilyMember member, NutritionDaySaveReq req) {
        Integer fromReq = req == null ? null : req.getCalorieTarget();
        if (fromReq != null) return Math.max(0, fromReq);
        Integer fromMember = member == null ? null : member.getCalorieTarget();
        if (fromMember != null) return Math.max(0, fromMember);
        return 2000;
    }

    private double resolveMacroTarget(Double fromMember, Double fromReq, double fallback) {
        if (fromReq != null && !Double.isNaN(fromReq)) return Math.max(0.0, fromReq);
        if (fromMember != null && !Double.isNaN(fromMember)) return Math.max(0.0, fromMember);
        return fallback;
    }

    private List<NutritionMealRecord> filterMeal(List<NutritionMealRecord> all, NutritionMealType type) {
        if (all == null || all.isEmpty()) return List.of();
        return all.stream().filter(r -> r != null && r.getMealType() == type).collect(Collectors.toList());
    }

    private List<NutritionFoodItemResp> toFoodList(List<NutritionMealRecord> records) {
        List<NutritionMealRecord> list = records == null ? List.of() : records;
        List<NutritionFoodItemResp> out = new ArrayList<>();
        for (NutritionMealRecord r : list) {
            if (r == null) continue;
            out.add(new NutritionFoodItemResp(
                    String.valueOf(r.getFoodName() == null ? "" : r.getFoodName()),
                    String.valueOf(r.getUnit() == null ? "" : r.getUnit()),
                    String.valueOf(r.getNutrientText() == null ? "" : r.getNutrientText()),
                    r.getCalorie() == null ? 0 : r.getCalorie(),
                    r.getCarb() == null ? 0.0 : r.getCarb(),
                    r.getProtein() == null ? 0.0 : r.getProtein(),
                    r.getFat() == null ? 0.0 : r.getFat(),
                    r.getFiber() == null ? 0.0 : r.getFiber(),
                    String.valueOf(r.getImageUrl() == null ? "" : r.getImageUrl()),
                    String.valueOf(r.getNote() == null ? "" : r.getNote())
            ));
        }
        return out;
    }

    private Macro sumMacros(List<NutritionMealRecord> all) {
        Macro out = new Macro(0.0, 0.0, 0.0, 0.0);
        if (all == null) return out;
        for (NutritionMealRecord r : all) {
            if (r == null) continue;
            out.carb += r.getCarb() == null ? 0.0 : r.getCarb();
            out.protein += r.getProtein() == null ? 0.0 : r.getProtein();
            out.fat += r.getFat() == null ? 0.0 : r.getFat();
            out.fiber += r.getFiber() == null ? 0.0 : r.getFiber();
        }
        return out;
    }

    private int sumCalories(List<NutritionMealRecord> all) {
        if (all == null) return 0;
        int sum = 0;
        for (NutritionMealRecord r : all) {
            if (r == null) continue;
            Integer c = r.getCalorie();
            if (c != null && c > 0) sum += c;
        }
        return Math.max(0, sum);
    }

    private int clampPct(double actual, double target) {
        double t = target <= 0 ? 1.0 : target;
        int pct = (int) Math.round((actual / t) * 100.0);
        if (pct < 0) return 0;
        return Math.min(100, pct);
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static class Macro {
        double carb;
        double protein;
        double fat;
        double fiber;

        Macro(double carb, double protein, double fat, double fiber) {
            this.carb = carb;
            this.protein = protein;
            this.fat = fat;
            this.fiber = fiber;
        }
    }
}
