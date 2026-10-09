package com.xs.ai.aiheathagent.controller;


import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;


public abstract class BaseController {
    protected final UserRepo userRepo;
    protected BaseController(UserRepo userRepo){ this.userRepo = userRepo; }
    protected User current(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth==null || auth.getPrincipal()==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) {
            if (u.getId() == null || u.getUid() == null || u.getUid().isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
            }
            return u;
        }
        String uid = principal.toString();
        return userRepo.findByUid(uid).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
    }

    protected String reqHeader(HttpServletRequest request, String name) {
        if (request == null || name == null || name.isBlank()) return "";
        try {
            String v = request.getHeader(name);
            return normalize(v, 256);
        } catch (Exception ignored) {
            return "";
        }
    }

    protected String reqIp(HttpServletRequest request) {
        if (request == null) return "";
        String xff = reqHeader(request, "X-Forwarded-For");
        if (!xff.isEmpty()) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) return normalize(first, 64);
        }
        String real = reqHeader(request, "X-Real-IP");
        if (!real.isEmpty()) return normalize(real, 64);
        try {
            return normalize(request.getRemoteAddr(), 64);
        } catch (Exception ignored) {
            return "";
        }
    }

    protected String reqUserAgent(HttpServletRequest request) {
        return reqHeader(request, "User-Agent");
    }

    protected String reqChannel(HttpServletRequest request) {
        String v = reqHeader(request, "X-Channel");
        if (!v.isEmpty()) return v;
        v = reqHeader(request, "X-WX-Channel");
        if (!v.isEmpty()) return v;
        return "";
    }

    protected String reqAppVersion(HttpServletRequest request) {
        return reqHeader(request, "X-App-Version");
    }

    protected String reqScene(HttpServletRequest request) {
        return reqHeader(request, "X-Scene");
    }

    protected String reqSessionId(HttpServletRequest request) {
        String sid = reqHeader(request, "X-Session-Id");
        if (!sid.isEmpty()) return normalize(sid, 128);
        return "";
    }

    private static String normalize(String v, int maxLen) {
        String s = String.valueOf(v == null ? "" : v).trim();
        if (s.isEmpty()) return "";
        int limit = Math.max(1, Math.min(maxLen, 2048));
        if (s.length() <= limit) return s;
        return s.substring(0, limit);
    }
}
