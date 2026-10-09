package com.xs.ai.aiheathagent.admin.controller;

import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.admin.service.AdminAuthService;
import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.xs.ai.aiheathagent.dto.req.LoginReq;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/admin/auth", "/api/admin/auth", "/api/api/admin/auth"})
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResp<?> login(@Valid @RequestBody LoginReq req) {
        var tp = adminAuthService.login(req.getAccount(), req.getPassword());
        AdminUser adminUser = adminAuthService.findAdminByAccount(req.getAccount())
                .orElseThrow(() -> new BizEx(ErrorCode.UNAUTHORIZED, "账号或密码错误"));

        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("id", adminUser.getId());
        admin.put("uid", adminUser.getUid());
        admin.put("nickname", adminUser.getNickname());
        admin.put("avatarUrl", adminUser.getAvatarUrl());
        admin.put("role", adminUser.getRole());
        admin.put("roleLevel", adminUser.getRoleLevel());
        String biz = String.valueOf(adminUser.getBizLine() == null ? "" : adminUser.getBizLine()).trim();
        admin.put("bizLine", biz.isEmpty() ? "all" : biz);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accessToken", tp.getAccessToken());
        out.put("refreshToken", tp.getRefreshToken());
        out.put("admin", admin);
        return ApiResp.ok(out);
    }

    @PostMapping("/token/refresh")
    public ApiResp<?> refresh(@RequestBody Map<String, String> body) {
        var tp = adminAuthService.refresh(body.getOrDefault("refreshToken", ""));
        return ApiResp.ok(tp);
    }

    @PostMapping("/logout")
    public ApiResp<?> logout(@RequestBody Map<String, String> body) {
        adminAuthService.logout(body.getOrDefault("refreshToken", ""));
        SecurityContextHolder.clearContext();
        return ApiResp.ok(Map.of());
    }
}
