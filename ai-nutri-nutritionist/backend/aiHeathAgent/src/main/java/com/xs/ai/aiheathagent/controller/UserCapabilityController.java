package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.conversation.expose.SystemStateSnapshot;
import com.xs.ai.aiheathagent.conversation.service.UserCapabilityCommandService;
import com.xs.ai.aiheathagent.conversation.service.UserRuntimeStateQueryService;
import com.xs.ai.aiheathagent.dto.req.CapabilitySetReq;
import com.xs.ai.aiheathagent.entity.CapabilityKey;
import com.xs.ai.aiheathagent.entity.CapabilityState;
import com.xs.ai.aiheathagent.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class UserCapabilityController extends BaseController {
    private final UserCapabilityCommandService userCapabilityCommandService;
    private final UserRuntimeStateQueryService userRuntimeStateQueryService;

    public UserCapabilityController(
            UserCapabilityCommandService userCapabilityCommandService,
            UserRuntimeStateQueryService userRuntimeStateQueryService,
            UserRepo userRepo
    ) {
        super(userRepo);
        this.userCapabilityCommandService = userCapabilityCommandService;
        this.userRuntimeStateQueryService = userRuntimeStateQueryService;
    }

    @GetMapping({"/api/user/capability", "/api/user/capability/", "/api/api/user/capability", "/api/api/user/capability/", "/user/capability", "/user/capability/"})
    public ResponseEntity<ApiResp<SystemStateSnapshot.CapabilityView>> get() {
        var user = current();
        SystemStateSnapshot snap = userRuntimeStateQueryService.loadRuntimeState(user.getId());
        return ResponseEntity.ok(ApiResp.ok(snap.capability()));
    }

    @PostMapping({"/api/user/capability/set", "/api/user/capability/set/", "/api/api/user/capability/set", "/api/api/user/capability/set/", "/user/capability/set", "/user/capability/set/"})
    public ResponseEntity<ApiResp<SystemStateSnapshot.CapabilityView>> set(@RequestBody CapabilitySetReq req) {
        var user = current();
        Long userId = user.getId();
        String keyRaw = req == null ? "" : String.valueOf(req.getCapabilityKey() == null ? "" : req.getCapabilityKey()).trim();
        String stateRaw = req == null ? "" : String.valueOf(req.getState() == null ? "" : req.getState()).trim();
        String reason = req == null ? "" : String.valueOf(req.getReason() == null ? "" : req.getReason());
        if (keyRaw.isEmpty()) {
            return ResponseEntity.ok(ApiResp.fail(400, "capabilityKey required"));
        }
        if (stateRaw.isEmpty()) {
            return ResponseEntity.ok(ApiResp.fail(400, "state required"));
        }
        CapabilityKey key;
        try {
            key = CapabilityKey.valueOf(keyRaw.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResp.fail(400, "invalid capabilityKey"));
        }
        CapabilityState state;
        try {
            state = CapabilityState.valueOf(stateRaw.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResp.fail(400, "invalid state"));
        }

        String source = "miniapp";
        if (state == CapabilityState.ENABLED) {
            userCapabilityCommandService.enable(userId, key, source, reason);
        } else if (state == CapabilityState.SUGGESTED) {
            userCapabilityCommandService.suggest(userId, key, source, reason);
        } else {
            userCapabilityCommandService.disable(userId, key, source, reason);
        }

        SystemStateSnapshot snap = userRuntimeStateQueryService.loadRuntimeState(userId);
        return ResponseEntity.ok(ApiResp.ok(snap.capability()));
    }
}

