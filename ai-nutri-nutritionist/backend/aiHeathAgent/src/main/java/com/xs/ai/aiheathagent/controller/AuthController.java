package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.xs.ai.aiheathagent.dto.req.*;
import com.xs.ai.aiheathagent.service.AuthService;
import com.xs.ai.aiheathagent.service.CodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/api/api/auth", "/auth"})
public class AuthController {
    private final CodeService codeService; private final AuthService authService;
    public AuthController(CodeService codeService, AuthService authService){ this.codeService = codeService; this.authService = authService; }

    @PostMapping("/code/send")
    public ApiResp<?> send(@Valid @RequestBody SendCodeReq req, HttpServletRequest request){
        String code = codeService.send(req.getAccount(), req.getScene(), request.getRemoteAddr());
        return ApiResp.ok(Map.of("debugCode", code));
    }

    @PostMapping("/register")
    public ApiResp<?> register(@Valid @RequestBody RegisterReq req){
        var data = authService.register(req.getAccount(), req.getCode(), req.getPassword(), req.getNickname());
        return ApiResp.ok(data);
    }

    @PostMapping("/login")
    public ApiResp<?> login(@Valid @RequestBody LoginReq req, HttpServletRequest request){
        var data = authService.passwordLogin(
                req.getAccount(),
                req.getPassword(),
                reqIp(request),
                reqUa(request),
                reqHeader(request, "X-Channel"),
                reqHeader(request, "X-App-Version"),
                reqHeader(request, "X-Scene"),
                reqHeader(request, "X-Session-Id")
        );
        return ApiResp.ok(data);
    }

    @PostMapping("/login/sms")
    public ApiResp<?> sms(@Valid @RequestBody SmsLoginReq req, HttpServletRequest request){
        var data = authService.smsLogin(
                req.getPhone(),
                req.getCode(),
                reqIp(request),
                reqUa(request),
                reqHeader(request, "X-Channel"),
                reqHeader(request, "X-App-Version"),
                reqHeader(request, "X-Scene"),
                reqHeader(request, "X-Session-Id")
        );
        return ApiResp.ok(data);
    }

    // 【新增】微信登录接口
    @PostMapping("/login/wechat")
    public ApiResp<?> wechatLogin(@Valid @RequestBody WechatLoginReq req, HttpServletRequest request){
        var data = authService.wechatLogin(
                req.getCode(),
                req.getNickname(),
                req.getAvatarUrl(),
                reqIp(request),
                reqUa(request),
                reqHeader(request, "X-Channel"),
                reqHeader(request, "X-App-Version"),
                reqHeader(request, "X-Scene"),
                reqHeader(request, "X-Session-Id")
        );
        return ApiResp.ok(data);
    }

    @PostMapping("/password/reset")
    public ApiResp<?> reset(@Valid @RequestBody ResetPwdReq req) {
        codeService.validateOrThrow(req.getAccount(), "reset_password", req.getCode());
        authService.updatePasswordByAccount(req.getAccount(), req.getNewPassword());
        codeService.markUsed(req.getAccount(), "reset_password");

        var user = authService.findUserByAnyAccount(req.getAccount())
                .orElseThrow(() -> new BizEx(ErrorCode.BIZ_ERROR, "账号不存在"));
        var tp = authService.issueTokens(user);
        return ApiResp.ok(tp);
    }

    @PostMapping("/token/refresh")
    public ApiResp<?> refresh(@RequestBody Map<String,String> body){
        var tp = authService.refresh(body.getOrDefault("refreshToken",""));
        return ApiResp.ok(tp);
    }

    @PostMapping("/logout")
    public ApiResp<?> logout(@RequestBody Map<String,String> body, HttpServletRequest request){
        authService.logout(body.getOrDefault("refreshToken",""));
        return ApiResp.ok(Map.of());
    }

    private static String reqHeader(HttpServletRequest request, String name) {
        if (request == null || name == null || name.isBlank()) return "";
        try {
            String v = request.getHeader(name);
            String s = String.valueOf(v == null ? "" : v).trim();
            if (s.isEmpty()) return "";
            return s.length() <= 256 ? s : s.substring(0, 256);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String reqIp(HttpServletRequest request) {
        if (request == null) return "";
        String xff = reqHeader(request, "X-Forwarded-For");
        if (!xff.isEmpty()) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) return first.length() <= 64 ? first : first.substring(0, 64);
        }
        String real = reqHeader(request, "X-Real-IP");
        if (!real.isEmpty()) return real.length() <= 64 ? real : real.substring(0, 64);
        try {
            String v = String.valueOf(request.getRemoteAddr() == null ? "" : request.getRemoteAddr()).trim();
            if (v.isEmpty()) return "";
            return v.length() <= 64 ? v : v.substring(0, 64);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String reqUa(HttpServletRequest request) {
        return reqHeader(request, "User-Agent");
    }
}
