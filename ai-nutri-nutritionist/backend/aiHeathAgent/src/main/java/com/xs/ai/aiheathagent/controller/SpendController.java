package com.xs.ai.aiheathagent.controller;


import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.req.SpendReq;
import com.xs.ai.aiheathagent.dto.resp.SpendResp;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.SpendService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController @RequestMapping("/api/wallet")
public class SpendController extends BaseController {
    private final SpendService spendService;
    public SpendController(UserRepo userRepo, SpendService spendService){ super(userRepo); this.spendService = spendService; }


    @PostMapping("/spend")
    public ApiResp<SpendResp> spend(@Valid @RequestBody SpendReq req){
        var u = current();
        return ApiResp.ok(spendService.spend(u.getId(), req));
    }
}