package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.req.NutritionDaySaveReq;
import com.xs.ai.aiheathagent.dto.req.NutritionMemberUpsertReq;
import com.xs.ai.aiheathagent.dto.resp.NutritionDayResp;
import com.xs.ai.aiheathagent.dto.resp.NutritionMemberResp;
import com.xs.ai.aiheathagent.dto.resp.OrganDetailResp;
import com.xs.ai.aiheathagent.dto.resp.OrganImpactResp;
import com.xs.ai.aiheathagent.dto.resp.RecipeDetailResp;
import com.xs.ai.aiheathagent.dto.resp.RecipeRecommendResp;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.OrganImpactService;
import com.xs.ai.aiheathagent.service.NutritionService;
import com.xs.ai.aiheathagent.service.RecipeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping({"/api/nutrition", "/api/api/nutrition", "/nutrition"})
public class NutritionController extends BaseController {
    private final NutritionService nutritionService;
    private final OrganImpactService organImpactService;
    private final RecipeService recipeService;
    private final UserEventRecordRepository userEventRecordRepository;

    public NutritionController(UserRepo userRepo, NutritionService nutritionService, OrganImpactService organImpactService, RecipeService recipeService, UserEventRecordRepository userEventRecordRepository) {
        super(userRepo);
        this.nutritionService = nutritionService;
        this.organImpactService = organImpactService;
        this.recipeService = recipeService;
        this.userEventRecordRepository = userEventRecordRepository;
    }

    @GetMapping("/members")
    public ApiResp<List<NutritionMemberResp>> members() {
        var u = current();
        return ApiResp.ok(nutritionService.listMembers(u.getId()));
    }

    @PostMapping("/members")
    public ApiResp<NutritionMemberResp> createMember(@Valid @RequestBody NutritionMemberUpsertReq req, HttpServletRequest httpRequest) {
        var u = current();
        NutritionMemberResp resp = nutritionService.createMember(u.getId(), req);
        trySaveFunnelEvent(u.getId(), httpRequest, "NUTRITION_MEMBER_CREATE", "memberId=" + (resp == null ? "0" : String.valueOf(resp.getId())));
        return ApiResp.ok(resp);
    }

    @DeleteMapping("/members/{id}")
    public ApiResp<?> deleteMember(@PathVariable Long id) {
        var u = current();
        nutritionService.deleteMember(u.getId(), id);
        return ApiResp.ok(java.util.Map.of());
    }

    @GetMapping("/day")
    public ApiResp<NutritionDayResp> getDay(
            @RequestParam Long memberId,
            @RequestParam(required = false) String date
    ) {
        var u = current();
        return ApiResp.ok(nutritionService.getDay(u.getId(), memberId, date));
    }

    @GetMapping("/organ-impact")
    public ApiResp<OrganImpactResp> organImpact(
            @RequestParam Long memberId,
            @RequestParam(required = false) String date
    ) {
        var u = current();
        return ApiResp.ok(organImpactService.getOrganImpact(u.getId(), memberId, date));
    }

    @GetMapping("/organ-detail")
    public ApiResp<OrganDetailResp> organDetail(
            @RequestParam Long memberId,
            @RequestParam(required = false) String organKey,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "false") boolean refreshAi
    ) {
        var u = current();
        return ApiResp.ok(organImpactService.getOrganDetail(u.getId(), memberId, organKey, date, refreshAi));
    }

    @GetMapping("/recipes/recommend")
    public ApiResp<RecipeRecommendResp> recommendRecipes(
            @RequestParam Long memberId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "6") Integer limit
    ) {
        var u = current();
        return ApiResp.ok(recipeService.recommend(u.getId(), memberId, date, limit));
    }

    @GetMapping("/recipes/{id}")
    public ApiResp<RecipeDetailResp> recipeDetail(
            @PathVariable Long id,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String date
    ) {
        var u = current();
        return ApiResp.ok(recipeService.detail(u.getId(), memberId, date, id));
    }

    @PostMapping("/day")
    public ApiResp<NutritionDayResp> saveDay(@Valid @RequestBody NutritionDaySaveReq req, HttpServletRequest httpRequest) {
        var u = current();
        NutritionDayResp resp = nutritionService.saveDay(u.getId(), req);
        String content = "memberId=" + (req == null || req.getMemberId() == null ? "0" : String.valueOf(req.getMemberId())) +
                ",date=" + (req == null ? "" : String.valueOf(req.getDate() == null ? "" : req.getDate()));
        trySaveFunnelEvent(u.getId(), httpRequest, "NUTRITION_DAY_SAVE", content);
        return ApiResp.ok(resp);
    }

    private void trySaveFunnelEvent(Long userId, HttpServletRequest request, String eventType, String contentText) {
        if (userId == null || request == null) return;
        try {
            UserEventRecord ev = new UserEventRecord();
            ev.setUserId(userId);
            String sid = reqSessionId(request);
            if (sid.isEmpty()) sid = "api_" + userId;
            ev.setSessionId(sid);
            ev.setOccurredAt(java.time.LocalDateTime.now());
            ev.setModality("API");
            ev.setEventType(String.valueOf(eventType == null ? "" : eventType).trim().isEmpty() ? "API_EVENT" : eventType);
            ev.setTopics("");
            ev.setConfidence(1.0);
            ev.setSuggestOpenCamera(false);
            ev.setSuggestWriteNutrition(false);
            ev.setContentText(String.valueOf(contentText == null ? "" : contentText));
            String channel = reqChannel(request);
            if (!channel.isEmpty()) ev.setChannel(channel);
            String appVersion = reqAppVersion(request);
            if (!appVersion.isEmpty()) ev.setAppVersion(appVersion);
            String scene = reqScene(request);
            if (!scene.isEmpty()) ev.setScene(scene);
            String ip = reqIp(request);
            if (!ip.isEmpty()) ev.setIp(ip);
            String ua = reqUserAgent(request);
            if (!ua.isEmpty()) ev.setUserAgent(ua);
            userEventRecordRepository.save(ev);
        } catch (Exception ignored) {
        }
    }
}
