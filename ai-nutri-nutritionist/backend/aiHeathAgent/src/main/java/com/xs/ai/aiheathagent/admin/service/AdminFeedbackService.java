package com.xs.ai.aiheathagent.admin.service;

import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.dto.resp.PageResp;
import com.xs.ai.aiheathagent.entity.UserFeedback;
import com.xs.ai.aiheathagent.repo.UserFeedbackRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminFeedbackService {
    private final AdminOperatorService adminOperatorService;
    private final UserFeedbackRepo userFeedbackRepo;

    public AdminFeedbackService(AdminOperatorService adminOperatorService, UserFeedbackRepo userFeedbackRepo) {
        this.adminOperatorService = adminOperatorService;
        this.userFeedbackRepo = userFeedbackRepo;
    }

    public PageResp<Map<String, Object>> page(int page, int pageSize, String q, Integer status, Long userId) {
        adminOperatorService.currentAdmin();
        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        Page<UserFeedback> pg = userFeedbackRepo.pageAdmin(safeStr(q), status, userId, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(this::toMap).toList();
        return new PageResp<>(pg.getTotalElements(), list);
    }

    public Map<String, Object> save(AdminUser admin, Long id, Integer status, String adminRemark) {
        UserFeedback fb = userFeedbackRepo.findById(id).orElse(null);
        if (fb == null) return Map.of("success", false, "reason", "not_found");

        int st = status == null ? fb.getStatus() == null ? 0 : fb.getStatus() : status;
        LocalDateTime now = LocalDateTime.now();
        fb.setAdminRemark(normalizeText(adminRemark, 2000));
        fb.setStatus(st);
        fb.setUpdatedAt(now);

        if (st == 1) {
            fb.setHandledByAdminId(admin.getId());
            if (fb.getHandledAt() == null) fb.setHandledAt(now);
        } else if (st == 0) {
            fb.setHandledByAdminId(null);
            fb.setHandledAt(null);
        }
        userFeedbackRepo.save(fb);
        return Map.of("success", true);
    }

    private Map<String, Object> toMap(UserFeedback f) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", f.getId());
        m.put("userId", f.getUserId());
        m.put("subject", safeStr(f.getSubject()));
        m.put("rating", f.getRating() == null ? 0 : f.getRating());
        m.put("content", safeStr(f.getContent()));
        m.put("orderNo", safeStr(f.getOrderNo()));
        m.put("status", f.getStatus() == null ? 0 : f.getStatus());
        m.put("adminRemark", safeStr(f.getAdminRemark()));
        m.put("handledByAdminId", f.getHandledByAdminId());
        m.put("handledAt", f.getHandledAt() == null ? "" : String.valueOf(f.getHandledAt()));
        m.put("createdAt", f.getCreatedAt() == null ? "" : String.valueOf(f.getCreatedAt()));
        m.put("updatedAt", f.getUpdatedAt() == null ? "" : String.valueOf(f.getUpdatedAt()));
        return m;
    }

    private static String normalizeText(String v, int maxLen) {
        String s = String.valueOf(v == null ? "" : v).trim();
        if (s.isEmpty()) return "";
        int limit = Math.max(1, Math.min(maxLen, 2000));
        if (s.length() <= limit) return s;
        return s.substring(0, limit);
    }

    private static String safeStr(Object v) {
        return String.valueOf(v == null ? "" : v).trim();
    }
}

