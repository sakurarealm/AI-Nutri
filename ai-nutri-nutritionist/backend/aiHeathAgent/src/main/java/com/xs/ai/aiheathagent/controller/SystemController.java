package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.conversation.service.TimeSlotTriggerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("")
public class SystemController {
    private final TimeSlotTriggerService timeSlotTriggerService;

    public SystemController(TimeSlotTriggerService timeSlotTriggerService) {
        this.timeSlotTriggerService = timeSlotTriggerService;
    }

    @GetMapping({"/api/system/ping", "/api/system/ping/", "/api/api/system/ping", "/api/api/system/ping/", "/system/ping", "/system/ping/", "/api/ping", "/api/ping/", "/ping", "/ping/"})
    public ApiResp<Map<String, Object>> ping() {
        return ApiResp.ok(Map.of(
                "ok", true,
                "ts", System.currentTimeMillis()
        ));
    }

    @PostMapping({"/api/system/tick", "/api/system/tick/", "/api/api/system/tick", "/api/api/system/tick/", "/system/tick", "/system/tick/"})
    public ApiResp<Map<String, Object>> tick() {
        timeSlotTriggerService.tick();
        return ApiResp.ok(Map.of("ok", true));
    }
}
