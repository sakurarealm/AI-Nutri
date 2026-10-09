package com.xs.ai.aiheathagent.admin.service;

import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.admin.repo.AdminUserRepo;
import com.xs.ai.aiheathagent.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminOperatorService {
    private final AdminUserRepo adminUserRepo;

    public AdminOperatorService(AdminUserRepo adminUserRepo) {
        this.adminUserRepo = adminUserRepo;
    }

    public AdminUser currentAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        Object principal = auth.getPrincipal();
        if (principal instanceof AdminUser u) {
            if (u.getId() == null || u.getUid() == null || u.getUid().isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
            AdminUser existed = adminUserRepo.findByUid(u.getUid()).orElse(null);
            if (existed != null) return existed;
            if (u.getId() != null) {
                AdminUser byId = adminUserRepo.findById(u.getId()).orElse(null);
                if (byId != null) return byId;
            }
            return u;
        }
        if (principal instanceof User u) {
            String role = safeStr(u.getRole()).trim().toUpperCase();
            if ("ADMIN".equals(role) && u.getId() != null && u.getUid() != null && !u.getUid().isBlank()) {
                AdminUser existed = adminUserRepo.findByUid(u.getUid()).orElse(null);
                if (existed != null) return existed;
                AdminUser byId = adminUserRepo.findById(u.getId()).orElse(null);
                if (byId != null) return byId;
                AdminUser a = new AdminUser();
                a.setId(u.getId());
                a.setUid(u.getUid());
                a.setUsername(u.getUid());
                a.setNickname(u.getNickname());
                a.setAvatarUrl(u.getAvatarUrl());
                a.setRole("ADMIN");
                a.setStatus(u.getStatus());
                a.setBizLine(u.getBizLine());
                return a;
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }

    public static int roleLevel(AdminUser admin) {
        if (admin == null) return 3;
        Integer vv = admin.getRoleLevel();
        if (vv == null) {
            String role = safeStr(admin.getRole()).trim().toUpperCase();
            if ("ADMIN".equals(role)) {
                String biz = normalizeBizLine(admin.getBizLine());
                return biz.isEmpty() ? 1 : 2;
            }
            return 3;
        }
        int v = vv;
        if (v < 1) return 3;
        if (v > 3) return 3;
        return v;
    }

    private static String safeStr(String v) {
        return String.valueOf(v == null ? "" : v).trim();
    }

    private static String normalizeBizLine(String v) {
        String s = safeStr(v);
        if (s.isEmpty() || "all".equalsIgnoreCase(s)) return "";
        return s;
    }
}

