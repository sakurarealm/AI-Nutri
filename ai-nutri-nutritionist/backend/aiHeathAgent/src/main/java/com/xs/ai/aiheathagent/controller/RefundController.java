package com.xs.ai.aiheathagent.controller;


import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.req.RefundReq;
import com.xs.ai.aiheathagent.dto.resp.RefundResp;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController @RequestMapping("/api/wallet")
public class RefundController extends BaseController {
    private final RefundService refundService;
    public RefundController(UserRepo userRepo, RefundService refundService){ super(userRepo); this.refundService = refundService; }


    @PostMapping("/refund")
    public ApiResp<RefundResp> refund(@Valid @RequestBody RefundReq req){
        var u = current();
        return ApiResp.ok(refundService.refund(u.getId(), req));
    }
}