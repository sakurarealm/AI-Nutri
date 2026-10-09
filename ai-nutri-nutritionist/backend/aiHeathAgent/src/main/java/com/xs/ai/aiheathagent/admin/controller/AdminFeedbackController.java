package com.xs.ai.aiheathagent.admin.controller;

import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.admin.service.AdminFeedbackService;
import com.xs.ai.aiheathagent.admin.service.AdminOperatorService;
import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.resp.PageResp;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping({"/api/admin/feedback", "/api/api/admin/feedback", "/admin/feedback"})
public class AdminFeedbackController {
    private final AdminOperatorService adminOperatorService;
    private final AdminFeedbackService adminFeedbackService;

    public AdminFeedbackController(AdminOperatorService adminOperatorService, AdminFeedbackService adminFeedbackService) {
        this.adminOperatorService = adminOperatorService;
        this.adminFeedbackService = adminFeedbackService;
    }

    @GetMapping({"", "/"})
    public ApiResp<PageResp<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId
    ) {
        return ApiResp.ok(adminFeedbackService.page(page, pageSize, q, status, userId));
    }

    @PostMapping({"/save", "/save/"})
    public ApiResp<Map<String, Object>> save(@RequestBody(required = false) Map<String, Object> body) {
        AdminUser admin = adminOperatorService.currentAdmin();
        Map<String, Object> b = body == null ? Map.of() : body;
        Long id = parseLongOrNull(b.get("id"));
        if (id == null || id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数错误");
        Integer status = parseIntOrNull(b.get("status"));
        String remark = String.valueOf(b.getOrDefault("adminRemark", "")).trim();
        Map<String, Object> res = adminFeedbackService.save(admin, id, status, remark);
        if (Boolean.FALSE.equals(res.get("success"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "不存在");
        }
        return ApiResp.ok(res);
    }

    private static Integer parseIntOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long parseLongOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return null;
        }
    }
}

