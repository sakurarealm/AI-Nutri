package com.xs.ai.aiheathagent.controller;


import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.req.CreateOrderReq;
import com.xs.ai.aiheathagent.dto.resp.*;
import com.xs.ai.aiheathagent.entity.RechargePackage;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.RechargeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController @RequestMapping("/api")
public class RechargeController extends BaseController {
    private final RechargeService rechargeService;
    public RechargeController(UserRepo userRepo, RechargeService rechargeService){ super(userRepo); this.rechargeService = rechargeService; }


    @GetMapping("/recharge/packages")
    public ApiResp<List<RechargePackageResp>> packages(){
        List<RechargePackage> list = rechargeService.listActive();
        List<RechargePackageResp> out = list.stream().map(p -> new RechargePackageResp(
                p.getId(), p.getPriceCny(), p.getKcoins(), p.getGift(), p.getIsRecommend()!=null && p.getIsRecommend()==1
        )).toList();
        return ApiResp.ok(out);
    }


    @PostMapping("/wallet/recharge")
    public ApiResp<CreateOrderResp> create(@Valid @RequestBody CreateOrderReq req){
        var u = current();
        return ApiResp.ok(rechargeService.createOrder(u.getId(), req));
    }
}