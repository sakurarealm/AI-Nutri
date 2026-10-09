package com.xs.ai.aiheathagent.controller;


import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.req.RedeemReq;
import com.xs.ai.aiheathagent.dto.resp.*;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController @RequestMapping("/api/wallet")
public class WalletController extends BaseController {
    private final WalletService walletService; private final PromoService promoService;
    public WalletController(UserRepo userRepo, WalletService walletService, PromoService promoService){ super(userRepo); this.walletService = walletService; this.promoService = promoService; }


    @GetMapping
    public ApiResp<WalletOverviewResp> overview(){
        var u = current();
        return ApiResp.ok(walletService.overview(u.getId()));
    }


    @GetMapping("/transactions")
    public ApiResp<PageResp<TxItemResp>> tx(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize){
        var u = current();
        return ApiResp.ok(walletService.pageTx(u.getId(), page, pageSize));
    }


    @PostMapping("/redeem")
    public ApiResp<?> redeem(@Valid @RequestBody RedeemReq req){
        var u = current();
        int bonus = promoService.redeem(u.getId(), req.getCode());
        return ApiResp.ok(java.util.Map.of("bonus", bonus));
    }
}