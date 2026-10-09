package com.xs.ai.aiheathagent.vision;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.conversation.model.ConversationInput;
import com.xs.ai.aiheathagent.conversation.service.ConversationOrchestratorService;
import com.xs.ai.aiheathagent.dto.resp.FoodCatalogItemResp;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.FoodCatalogService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FoodVisionController {
    private final FoodVisionService foodVisionService;
    private final ConversationOrchestratorService orchestratorService;
    private final UserRepo userRepo;
    private final FoodCatalogService foodCatalogService;

    @PostMapping(
            value = {"/api/food/recognize", "/api/food/recognize/", "/api/api/food/recognize", "/api/api/food/recognize/", "/food/recognize", "/food/recognize/"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResp<RecognizeFoodResp> recognize(
            @NotNull @RequestPart("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionIdHeader
    ) throws Exception {
        List<FoodCandidate> items = foodVisionService.recognizeDish(file);
        enrichTopWithMacros(items);
        User u = currentOrNull();
        Long uid = resolveUserId(userId, userIdHeader, u);
        String sid = resolveSessionId(sessionId, sessionIdHeader, uid);
        orchestratorService.handleUserInput(u, ConversationInput.image(sid, uid, items));
        RecognizeFoodResp out = new RecognizeFoodResp();
        out.setItems(items);
        return ApiResp.ok(out);
    }

    private void enrichTopWithMacros(List<FoodCandidate> items) {
        if (items == null || items.isEmpty()) return;
        items.sort(Comparator.comparingDouble((FoodCandidate c) -> safeProbability(c)).reversed());
        FoodCandidate top = items.get(0);
        String name = String.valueOf(top == null ? "" : top.getName()).trim();
        if (name.isEmpty()) return;
        List<FoodCatalogItemResp> list = foodCatalogService.search(name, 8);
        FoodCatalogItemResp best = pickBest(name, list);
        if (best == null) return;
        if (top.getCarb() == null) top.setCarb(best.getCarb());
        if (top.getProtein() == null) top.setProtein(best.getProtein());
        if (top.getFat() == null) top.setFat(best.getFat());
        if (top.getCalorie() == null || top.getCalorie() <= 0) top.setCalorie((double) best.getCalorie());
    }

    private double safeProbability(FoodCandidate c) {
        if (c == null || c.getProbability() == null || c.getProbability().isNaN()) return 0.0;
        return Math.max(0.0, Math.min(1.0, c.getProbability()));
    }

    private FoodCatalogItemResp pickBest(String q, List<FoodCatalogItemResp> list) {
        String query = normName(q);
        if (query.isEmpty() || list == null || list.isEmpty()) return null;
        FoodCatalogItemResp best = null;
        int bestScore = -1;
        for (FoodCatalogItemResp it : list) {
            if (it == null) continue;
            String name = String.valueOf(it.getName() == null ? "" : it.getName()).trim();
            if (name.isEmpty()) continue;
            String n = normName(name);
            int score = matchScore(query, n);
            if (score > bestScore) {
                bestScore = score;
                best = it;
            }
        }
        return bestScore <= 0 ? null : best;
    }

    private String normName(String s) {
        String t = String.valueOf(s == null ? "" : s).trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return "";
        return t.replaceAll("[\\s()（）\\[\\]【】,，.。·•\\-_/]+", "");
    }

    private int matchScore(String query, String candidate) {
        if (query.isEmpty() || candidate.isEmpty()) return 0;
        if (candidate.equals(query)) return 4;
        if (candidate.startsWith(query) || query.startsWith(candidate)) return 3;
        if (candidate.contains(query) || query.contains(candidate)) return 2;
        return 1;
    }

    private User currentOrNull() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal() == null) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof User u) {
                return u;
            }
            String uid = principal.toString();
            return userRepo.findByUid(uid).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveUserId(String userId, String headerUserId, User currentUser) {
        try {
            String v = String.valueOf(userId == null ? "" : userId).trim();
            if (!v.isEmpty() && v.matches("^\\d+$")) return Long.parseLong(v);
            String h = String.valueOf(headerUserId == null ? "" : headerUserId).trim();
            if (!h.isEmpty() && h.matches("^\\d+$")) return Long.parseLong(h);
            if (currentUser != null && currentUser.getId() != null) return currentUser.getId();
        } catch (Exception ignored) {
        }
        return 1L;
    }

    private String resolveSessionId(String sessionId, String headerSessionId, Long userId) {
        String v = String.valueOf(sessionId == null ? "" : sessionId).trim();
        if (!v.isEmpty()) return v;
        String h = String.valueOf(headerSessionId == null ? "" : headerSessionId).trim();
        if (!h.isEmpty()) return h;
        return "vision_" + String.valueOf(userId == null ? 1L : userId);
    }

    @Data
    public static class RecognizeFoodResp {
        private List<FoodCandidate> items;
    }
}
