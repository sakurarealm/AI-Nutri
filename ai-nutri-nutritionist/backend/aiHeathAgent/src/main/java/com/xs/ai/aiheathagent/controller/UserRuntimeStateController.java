package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.conversation.expose.SystemStateSnapshot;
import com.xs.ai.aiheathagent.conversation.service.UserRuntimeStateQueryService;
import com.xs.ai.aiheathagent.entity.UserEventRecord;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import com.xs.ai.aiheathagent.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class UserRuntimeStateController extends BaseController {
    private final UserRuntimeStateQueryService userRuntimeStateQueryService;
    private final UserEventRecordRepository userEventRecordRepository;

    public UserRuntimeStateController(UserRuntimeStateQueryService userRuntimeStateQueryService, UserEventRecordRepository userEventRecordRepository, UserRepo userRepo) {
        super(userRepo);
        this.userRuntimeStateQueryService = userRuntimeStateQueryService;
        this.userEventRecordRepository = userEventRecordRepository;
    }

    @GetMapping({"/api/user/runtime-state", "/api/user/runtime-state/", "/api/api/user/runtime-state", "/api/api/user/runtime-state/", "/user/runtime-state", "/user/runtime-state/"})
    public ResponseEntity<ApiResp<SystemStateSnapshot>> runtimeState() {
        var user = current();
        SystemStateSnapshot out = userRuntimeStateQueryService.loadRuntimeState(user.getId());
        return ResponseEntity.ok(ApiResp.ok(out));
    }

    @PostMapping({"/api/user/event", "/api/user/event/", "/api/api/user/event", "/api/api/user/event/", "/user/event", "/user/event/"})
    public ResponseEntity<ApiResp<Map<String, Object>>> track(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        var user = current();
        UserEventRecord ev = new UserEventRecord();
        ev.setUserId(user.getId());
        String sid = reqSessionId(request);
        if (sid.isEmpty()) sid = "client_" + user.getId();
        ev.setSessionId(sid);
        ev.setOccurredAt(LocalDateTime.now());
        String modality = String.valueOf(body == null ? "" : body.getOrDefault("modality", "")).trim();
        ev.setModality(modality.isEmpty() ? "CLIENT" : modality);
        String eventType = String.valueOf(body == null ? "" : body.getOrDefault("eventType", "")).trim();
        ev.setEventType(eventType.isEmpty() ? "CLIENT_EVENT" : eventType);
        ev.setTopics(String.valueOf(body == null ? "" : body.getOrDefault("topics", "")).trim());
        ev.setConfidence(toDouble(body == null ? null : body.get("confidence"), 1.0));
        ev.setSuggestOpenCamera(toBool(body == null ? null : body.get("suggestOpenCamera"), false));
        ev.setSuggestWriteNutrition(toBool(body == null ? null : body.get("suggestWriteNutrition"), false));
        ev.setContentText(String.valueOf(body == null ? "" : body.getOrDefault("contentText", "")));
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
        try {
            userEventRecordRepository.save(ev);
        } catch (Exception ignored) {
        }
        return ResponseEntity.ok(ApiResp.ok(Map.of("ok", true, "eventId", ev.getId())));
    }

    private static boolean toBool(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim().toLowerCase();
        if (s.isEmpty()) return def;
        return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("y");
    }

    private static double toDouble(Object v, double def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return def;
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            return def;
        }
    }
}
