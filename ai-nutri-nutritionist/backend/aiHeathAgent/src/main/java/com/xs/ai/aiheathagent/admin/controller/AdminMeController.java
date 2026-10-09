package com.xs.ai.aiheathagent.admin.controller;

import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.admin.repo.AdminUserRepo;
import com.xs.ai.aiheathagent.admin.service.AdminOperatorService;
import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.service.CodeService;
import com.xs.ai.aiheathagent.util.AccountUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/admin/me", "/api/admin/me", "/api/api/admin/me"})
public class AdminMeController {
    private final AdminOperatorService adminOperatorService;
    private final AdminUserRepo adminUserRepo;
    private final CodeService codeService;

    public AdminMeController(AdminOperatorService adminOperatorService, AdminUserRepo adminUserRepo, CodeService codeService) {
        this.adminOperatorService = adminOperatorService;
        this.adminUserRepo = adminUserRepo;
        this.codeService = codeService;
    }

    @GetMapping
    public ApiResp<Map<String, Object>> me(HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        AdminUser me = adminUserRepo.findById(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
        return ApiResp.ok(toMeResp(me));
    }

    @PostMapping("/update")
    @Transactional
    public ApiResp<Map<String, Object>> update(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        if (body == null) body = Map.of();

        AdminUser me = adminUserRepo.findById(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));

        String nickname = safeStr(body.containsKey("nickname") ? body.get("nickname") : body.get("realName"));
        if (nickname.length() > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "昵称过长");

        if (!nickname.isEmpty()) me.setNickname(nickname);

        me.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(me);
        return ApiResp.ok(toMeResp(me));
    }

    @PostMapping("/bind/code/send")
    public ApiResp<Map<String, Object>> sendBindCode(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        if (body == null) body = Map.of();

        String type = safeStr(body.get("type")).toLowerCase();
        String target = safeStr(body.get("target"));
        String scene;
        String normalizedTarget;

        if ("phone".equals(type)) {
            normalizedTarget = AccountUtil.normalizePhone(target);
            if (normalizedTarget == null || !AccountUtil.isPhone(normalizedTarget)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号格式不正确");
            }
            scene = "admin_bind_phone";
        } else if ("email".equals(type)) {
            normalizedTarget = AccountUtil.normalizeAccount(target);
            if (normalizedTarget == null || !AccountUtil.isEmail(normalizedTarget)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
            }
            scene = "admin_bind_email";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "类型不支持");
        }

        String debugCode = codeService.send(normalizedTarget, scene, req.getRemoteAddr());
        return ApiResp.ok(Map.of("success", true, "debugCode", debugCode));
    }

    @PostMapping("/bind/phone")
    @Transactional
    public ApiResp<Map<String, Object>> bindPhone(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        if (body == null) body = Map.of();

        AdminUser me = adminUserRepo.findById(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));

        String phone = safeStr(body.get("phoneNumber"));
        String code = safeStr(body.get("code"));
        String normalizedPhone = AccountUtil.normalizePhone(phone);
        if (normalizedPhone == null || !AccountUtil.isPhone(normalizedPhone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号格式不正确");
        }
        if (code.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入验证码");

        codeService.validateOrThrow(normalizedPhone, "admin_bind_phone", code);

        var existed = adminUserRepo.findByPhoneNumber(normalizedPhone).orElse(null);
        if (existed != null && !existed.getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "手机号已被绑定");
        }

        me.setPhoneNumber(normalizedPhone);
        me.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(me);
        codeService.markUsed(normalizedPhone, "admin_bind_phone");
        return ApiResp.ok(toMeResp(me));
    }

    @PostMapping("/bind/email")
    @Transactional
    public ApiResp<Map<String, Object>> bindEmail(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        if (body == null) body = Map.of();

        AdminUser me = adminUserRepo.findById(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));

        String email = safeStr(body.get("email"));
        String code = safeStr(body.get("code"));
        String normalizedEmail = AccountUtil.normalizeAccount(email);
        if (normalizedEmail == null || !AccountUtil.isEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
        }
        if (code.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入验证码");

        codeService.validateOrThrow(normalizedEmail, "admin_bind_email", code);

        var existed = adminUserRepo.findByEmail(normalizedEmail).orElse(null);
        if (existed != null && !existed.getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被绑定");
        }

        me.setEmail(normalizedEmail);
        me.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(me);
        codeService.markUsed(normalizedEmail, "admin_bind_email");
        return ApiResp.ok(toMeResp(me));
    }

    @PostMapping("/unbind/code/send")
    public ApiResp<Map<String, Object>> sendUnbindCode(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        if (body == null) body = Map.of();

        AdminUser me = adminUserRepo.findById(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));

        String type = safeStr(body.get("type")).toLowerCase();
        String target;
        String scene;

        if ("phone".equals(type)) {
            target = safeStr(me.getPhoneNumber());
            if (target.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未绑定手机号");
            scene = "admin_unbind_phone";
        } else if ("email".equals(type)) {
            target = safeStr(me.getEmail());
            if (target.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未绑定邮箱");
            scene = "admin_unbind_email";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "类型不支持");
        }

        String debugCode = codeService.send(target, scene, req.getRemoteAddr());
        return ApiResp.ok(Map.of("success", true, "debugCode", debugCode));
    }

    @PostMapping("/unbind/phone")
    @Transactional
    public ApiResp<Map<String, Object>> unbindPhone(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        if (body == null) body = Map.of();

        AdminUser me = adminUserRepo.findById(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));

        String boundPhone = safeStr(me.getPhoneNumber());
        if (boundPhone.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未绑定手机号");

        String code = safeStr(body.get("code"));
        if (code.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入验证码");

        codeService.validateOrThrow(boundPhone, "admin_unbind_phone", code);

        me.setPhoneNumber(null);
        me.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(me);
        codeService.markUsed(boundPhone, "admin_unbind_phone");
        return ApiResp.ok(toMeResp(me));
    }

    @PostMapping("/unbind/email")
    @Transactional
    public ApiResp<Map<String, Object>> unbindEmail(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        if (body == null) body = Map.of();

        AdminUser me = adminUserRepo.findById(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));

        String boundEmail = safeStr(me.getEmail());
        if (boundEmail.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未绑定邮箱");

        String code = safeStr(body.get("code"));
        if (code.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入验证码");

        codeService.validateOrThrow(boundEmail, "admin_unbind_email", code);

        me.setEmail(null);
        me.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(me);
        codeService.markUsed(boundEmail, "admin_unbind_email");
        return ApiResp.ok(toMeResp(me));
    }

    @PostMapping("/avatar")
    @Transactional
    public ApiResp<Map<String, Object>> updateAvatar(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = adminOperatorService.currentAdmin();
        if (body == null) body = Map.of();

        AdminUser me = adminUserRepo.findById(operator.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));

        String base64 = safeStr(body.containsKey("avatarBase64") ? body.get("avatarBase64") : body.get("avatarUrl"));
        if (base64.length() > 15_000_000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像过大");
        if (!base64.isEmpty() && !base64.startsWith("data:image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像格式不支持");
        }
        me.setAvatarUrl(base64);
        me.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(me);
        return ApiResp.ok(toMeResp(me));
    }

    private static Map<String, Object> toMeResp(AdminUser me) {
        int lvl = AdminOperatorService.roleLevel(me);
        String roleText = switch (lvl) {
            case 1 -> "一级";
            case 2 -> "二级";
            default -> "三级";
        };
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", me.getId());
        out.put("username", safeStr(me.getUsername()));
        out.put("realName", safeStr(me.getNickname()));
        out.put("nickname", safeStr(me.getNickname()));
        out.put("phoneNumber", safeStr(me.getPhoneNumber()));
        out.put("email", safeStr(me.getEmail()));
        out.put("avatarUrl", safeStr(me.getAvatarUrl()));
        out.put("roleLevel", lvl);
        out.put("roleList", List.of(roleText));
        String biz = safeStr(me.getBizLine());
        out.put("bizLine", biz.isEmpty() ? "all" : biz);
        return out;
    }

    private static String safeStr(Object v) {
        return String.valueOf(v == null ? "" : v).trim();
    }
}
