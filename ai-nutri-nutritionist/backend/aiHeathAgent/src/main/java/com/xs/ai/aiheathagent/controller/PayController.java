package com.xs.ai.aiheathagent.controller;


import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.PayService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;


import java.util.Map;


@RestController @RequestMapping("/api/pay")
public class PayController extends BaseController {
    private final PayService payService;
    public PayController(UserRepo userRepo, PayService payService){ super(userRepo); this.payService = payService; }


    // 开发用：前端在模拟支付成功后，直接调用这里入账
    @PostMapping("/mock")
    public ApiResp<?> mock(@RequestBody Map<String, String> body){
        current(); // 需要鉴权
        String orderNo = body.getOrDefault("orderNo", "");
        if (orderNo.isBlank()) throw new IllegalArgumentException("orderNo 不能为空");
        payService.markPaid(orderNo, "{\"mock\":true}");
        return ApiResp.ok(Map.of());
    }


    // 回调模拟：免鉴权，第三方支付平台通常以服务端回调
    @PostMapping("/notify/test")
    public ApiResp<?> notifyTest(@RequestParam @NotBlank String orderNo, @RequestParam(defaultValue="true") boolean success){
        if (success) payService.markPaid(orderNo, "{\"notify\":true}");
        return ApiResp.ok(Map.of());
    }
}