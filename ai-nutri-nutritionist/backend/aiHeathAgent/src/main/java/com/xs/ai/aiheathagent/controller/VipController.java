package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.req.VipCreateOrderReq;
import com.xs.ai.aiheathagent.dto.resp.VipOrderResp;
import com.xs.ai.aiheathagent.dto.resp.VipPlanResp;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/vip", "/api/api/vip", "/vip"})
public class VipController extends BaseController {
    private final VipService vipService;
    private final UserEventRecordRepository userEventRecordRepository;

    public VipController(UserRepo userRepo, VipService vipService, UserEventRecordRepository userEventRecordRepository) {
        super(userRepo);
        this.vipService = vipService;
        this.userEventRecordRepository = userEventRecordRepository;
    }

    @GetMapping("/plans")
    public ApiResp<List<VipPlanResp>> plans() {
        current();
        return ApiResp.ok(vipService.plans());
    }

    @PostMapping("/orders")
    public ApiResp<VipOrderResp> createOrder(@Valid @RequestBody VipCreateOrderReq req, HttpServletRequest httpRequest) {
        var u = current();
        VipOrderResp resp = vipService.createOrder(u.getId(), req);
        trySaveFunnelEvent(u.getId(), httpRequest, "VIP_ORDER_CREATE", "orderId=" + (resp == null ? "0" : String.valueOf(resp.getId())));
        return ApiResp.ok(resp);
    }

    @GetMapping("/orders/{orderId}")
    public ApiResp<VipOrderResp> order(@PathVariable Long orderId) {
        var u = current();
        return ApiResp.ok(vipService.getOrderOrThrow(u.getId(), orderId));
    }

    @PostMapping("/orders/{orderId}/pay")
    public ApiResp<Map<String, Object>> pay(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        var u = current();
        VipOrderResp paid = vipService.payOrder(u.getId(), orderId, "{\"mock\":true}");
        trySaveFunnelEvent(u.getId(), httpRequest, "VIP_ORDER_PAY", "orderId=" + String.valueOf(orderId));
        return ApiResp.ok(Map.of(
                "order", paid,
                "mockPaid", true
        ));
    }

    @PostMapping("/pay/notify")
    public ApiResp<?> notify(@RequestParam @NotBlank String orderNo, @RequestParam(defaultValue = "true") boolean success) {
        vipService.notifyPaid(orderNo, success, "{\"notify\":true}");
        return ApiResp.ok(Map.of());
    }

    private void trySaveFunnelEvent(Long userId, HttpServletRequest request, String eventType, String contentText) {
        if (userId == null || request == null) return;
        try {
            UserEventRecord ev = new UserEventRecord();
            ev.setUserId(userId);
            String sid = reqSessionId(request);
            if (sid.isEmpty()) sid = "api_" + userId;
            ev.setSessionId(sid);
            ev.setOccurredAt(LocalDateTime.now());
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
