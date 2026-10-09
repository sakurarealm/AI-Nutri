package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.entity.UserFeedback;
import com.xs.ai.aiheathagent.repo.UserFeedbackRepo;
import com.xs.ai.aiheathagent.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping({"/api/feedback", "/api/api/feedback", "/feedback"})
public class FeedbackController extends BaseController {
    private final UserFeedbackRepo userFeedbackRepo;

    public FeedbackController(UserRepo userRepo, UserFeedbackRepo userFeedbackRepo) {
        super(userRepo);
        this.userFeedbackRepo = userFeedbackRepo;
    }

    @PostMapping({"/submit", "/submit/", "", "/"})
    public ApiResp<Map<String, Object>> submit(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest req) {
        User u = current();
        Map<String, Object> b = body == null ? Map.of() : body;

        String subject = normalizeText(b.get("subject"), 64);
        if (subject.isEmpty()) subject = "其他问题";
        String content = normalizeText(b.get("content"), 2000);
        if (content.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入反馈内容");
        int rating = normalizeInt(b.get("rating"), 5);
        if (rating < 1 || rating > 5) rating = 5;
        String orderNo = normalizeText(b.get("orderNo"), 64);
        if (orderNo.isEmpty()) orderNo = normalizeText(b.get("orderId"), 64);

        LocalDateTime now = LocalDateTime.now();
        UserFeedback fb = new UserFeedback();
        fb.setUserId(u.getId());
        fb.setSubject(subject);
        fb.setContent(content);
        fb.setRating(rating);
        fb.setOrderNo(orderNo);
        fb.setStatus(0);
        fb.setCreatedAt(now);
        fb.setUpdatedAt(now);

        userFeedbackRepo.save(fb);
        return ApiResp.ok(Map.of("id", fb.getId(), "success", true));
    }

    private static String normalizeText(Object v, int maxLen) {
        String s = String.valueOf(v == null ? "" : v).trim();
        if (s.isEmpty()) return "";
        int limit = Math.max(1, Math.min(maxLen, 2000));
        if (s.length() <= limit) return s;
        return s.substring(0, limit);
    }

    private static int normalizeInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return def;
        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return def;
        }
    }
}

