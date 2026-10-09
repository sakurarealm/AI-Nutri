package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.req.FoodPortionCalcReq;
import com.xs.ai.aiheathagent.dto.req.NutritionFoodPickReq;
import com.xs.ai.aiheathagent.dto.resp.FoodCatalogItemResp;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.FoodCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/nutrition/foods", "/api/api/nutrition/foods", "/nutrition/foods"})
public class FoodCatalogController extends BaseController {
    private final FoodCatalogService foodCatalogService;

    public FoodCatalogController(UserRepo userRepo, FoodCatalogService foodCatalogService) {
        super(userRepo);
        this.foodCatalogService = foodCatalogService;
    }

    @GetMapping("/recommend")
    public ApiResp<List<FoodCatalogItemResp>> recommend(
            @RequestParam(required = false) String mealType,
            @RequestParam(defaultValue = "12") int limit
    ) {
        current();
        return ApiResp.ok(foodCatalogService.recommend(mealType, limit));
    }

    @GetMapping("/search")
    public ApiResp<List<FoodCatalogItemResp>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "30") int limit
    ) {
        current();
        return ApiResp.ok(foodCatalogService.search(q, limit));
    }

    @PostMapping("/portion")
    public ApiResp<FoodCatalogItemResp> calcPortion(@RequestBody(required = false) FoodPortionCalcReq req) {
        current();
        return ApiResp.ok(foodCatalogService.calcPortion(req));
    }

    @PostMapping("/picks")
    public ApiResp<Map<String, Object>> recordPick(@RequestBody(required = false) NutritionFoodPickReq req) {
        var u = current();
        foodCatalogService.recordPick(u.getId(), req);
        return ApiResp.ok(Map.of("ok", true));
    }

    @GetMapping("/{id}")
    public ApiResp<FoodCatalogItemResp> getById(@PathVariable Long id) {
        current();
        return ApiResp.ok(foodCatalogService.calcPortion(buildDefaultReq(id)));
    }

    private FoodPortionCalcReq buildDefaultReq(Long id) {
        FoodPortionCalcReq req = new FoodPortionCalcReq();
        req.setFoodId(id);
        req.setMultiplier(1.0);
        req.setUnitLabel("1份");
        return req;
    }
}

