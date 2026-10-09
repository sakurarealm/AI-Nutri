package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.conversation.model.ConversationInput;
import com.xs.ai.aiheathagent.dto.ChatRequest;
import com.xs.ai.aiheathagent.dto.ChatResponse;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.entity.InboxAckRecord;
import com.xs.ai.aiheathagent.repository.AuthRefreshTokenRepository;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.InboxAckRecordRepository;
import com.xs.ai.aiheathagent.repository.SystemMessageMetaRecordRepository;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.conversation.service.ConversationOrchestratorService;
import com.xs.ai.aiheathagent.conversation.service.FollowupSuggestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class ChatController extends BaseController {
    private final ConversationOrchestratorService orchestratorService;
    private final AuthRefreshTokenRepository tokenRepository;
    private final ConversationRecordRepository conversationRepository;
    private final InboxAckRecordRepository inboxAckRecordRepository;
    private final SystemMessageMetaRecordRepository systemMessageMetaRecordRepository;
    private final FollowupSuggestionService followupSuggestionService;
    private final ObjectMapper objectMapper;
    private final UserEventRecordRepository userEventRecordRepository;
    private static final String ONBOARDING_SESSION_PREFIX = "onb_";
    private static final String ONBOARDING_KICKOFF_TOKEN = "__ONBOARDING_KICKOFF__";

    public ChatController(ConversationOrchestratorService orchestratorService, AuthRefreshTokenRepository tokenRepository, ConversationRecordRepository conversationRepository, InboxAckRecordRepository inboxAckRecordRepository, SystemMessageMetaRecordRepository systemMessageMetaRecordRepository, FollowupSuggestionService followupSuggestionService, ObjectMapper objectMapper, UserEventRecordRepository userEventRecordRepository, UserRepo userRepo) {
        super(userRepo);
        this.orchestratorService = orchestratorService;
        this.tokenRepository = tokenRepository;
        this.conversationRepository = conversationRepository;
        this.inboxAckRecordRepository = inboxAckRecordRepository;
        this.systemMessageMetaRecordRepository = systemMessageMetaRecordRepository;
        this.followupSuggestionService = followupSuggestionService;
        this.objectMapper = objectMapper;
        this.userEventRecordRepository = userEventRecordRepository;
    }

    @PostMapping({"/api/chat", "/api/chat/", "/api/api/chat", "/api/api/chat/", "/chat", "/chat/"})
    public ResponseEntity<ChatResponse> chat(@RequestHeader(value = "Authorization", required = false) String authHeader, @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty() || request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("请求参数不完整"));
        }

        com.xs.ai.aiheathagent.entity.User user = null;
        try {
            user = current();
        } catch (Exception ignored) {
            user = null;
        }

        Long uid;
        try {
            String incoming = String.valueOf(request.getUserId() == null ? "" : request.getUserId()).trim();
            if (!incoming.isEmpty() && incoming.matches("^\\d+$")) {
                uid = Long.parseLong(incoming);
            } else if (user != null && user.getId() != null) {
                uid = user.getId();
            } else {
                uid = 1L;
            }
        } catch (Exception ignored) {
            uid = 1L;
        }
        if (user == null || user.getId() == null || !user.getId().equals(uid)) {
            try {
                user = userRepo.findById(uid).orElse(null);
            } catch (Exception ignored) {
                user = null;
            }
            if (user == null) {
                user = new com.xs.ai.aiheathagent.entity.User();
                user.setId(uid);
            }
        }
        var input = ConversationInput.text(request.getSessionId(), uid, request.getMessage());
        try {
            var result = orchestratorService.handleUserInput(user, input);
            tryUpdateEventMeta(result == null || result.event() == null ? null : result.event().eventId(), httpRequest, request.getSessionId());
            ChatResponse resp = new ChatResponse(result.reply() == null ? "" : String.valueOf(result.reply().text()));
            resp.setSystemMessageType(result.systemMessageType());
            resp.setSnapshot(result.snapshot());
            if (isOnboardingKickoff(request.getSessionId(), request.getMessage())) {
                resp.setMeta(buildOnboardingKickoffMeta(uid));
                resp.setSuggestions(List.of());
            } else if (result.systemMessageType() == com.xs.ai.aiheathagent.conversation.expose.SystemMessageType.CHAT) {
                resp.setSuggestions(followupSuggestionService.suggest(request.getSessionId(), uid, request.getMessage(), resp.getReply()));
            } else {
                resp.setSuggestions(List.of());
            }
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            ChatResponse resp = new ChatResponse("我这会儿有点忙，但我在。你把目标和今天吃的东西发我，我先给你一版建议。");
            resp.setSuggestions(List.of(
                    "我想减脂，怎么安排",
                    "我今天吃了这些…",
                    "身高体重是…"
            ));
            return ResponseEntity.ok(resp);
        }
    }

    @PostMapping(
            value = {"/api/chat/stream", "/api/chat/stream/", "/api/api/chat/stream", "/api/api/chat/stream/", "/chat/stream", "/chat/stream/"},
            produces = "application/x-ndjson"
    )
    public ResponseEntity<StreamingResponseBody> chatStream(@RequestHeader(value = "Authorization", required = false) String authHeader, @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        StreamingResponseBody body = (outputStream) -> {
            ObjectMapper mapper = objectMapper;
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty() || request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
                writer.write(mapper.writeValueAsString(Map.of("type", "error", "message", "请求参数不完整")) + "\n");
                writer.flush();
                return;
            }

            com.xs.ai.aiheathagent.entity.User user = null;
            try {
                user = current();
            } catch (Exception ignored) {
                user = null;
            }

            Long uid;
            try {
                String incoming = String.valueOf(request.getUserId() == null ? "" : request.getUserId()).trim();
                if (!incoming.isEmpty() && incoming.matches("^\\d+$")) {
                    uid = Long.parseLong(incoming);
                } else if (user != null && user.getId() != null) {
                    uid = user.getId();
                } else {
                    uid = 1L;
                }
            } catch (Exception ignored) {
                uid = 1L;
            }
            if (user == null || user.getId() == null || !user.getId().equals(uid)) {
                try {
                    user = userRepo.findById(uid).orElse(null);
                } catch (Exception ignored) {
                    user = null;
                }
                if (user == null) {
                    user = new com.xs.ai.aiheathagent.entity.User();
                    user.setId(uid);
                }
            }

            var input = ConversationInput.text(request.getSessionId(), uid, request.getMessage());
            try {
                boolean onboardingKickoff = isOnboardingKickoff(request.getSessionId(), request.getMessage());
                var result = orchestratorService.handleUserInputStream(user, input, (delta) -> {
                    try {
                        String d = String.valueOf(delta == null ? "" : delta);
                        if (d.isEmpty()) return;
                        writer.write(mapper.writeValueAsString(Map.of("type", "delta", "text", d)) + "\n");
                        writer.flush();
                    } catch (Exception ignored) {
                    }
                }, false);
                tryUpdateEventMeta(result == null || result.event() == null ? null : result.event().eventId(), httpRequest, request.getSessionId());

                ChatResponse resp = new ChatResponse(result.reply() == null ? "" : String.valueOf(result.reply().text()));
                resp.setSystemMessageType(result.systemMessageType());
                if (onboardingKickoff) {
                    resp.setMeta(buildOnboardingKickoffMeta(uid));
                }

                Map<String, Object> finalPayload = new HashMap<>();
                finalPayload.put("type", "final");
                finalPayload.put("reply", resp.getReply());
                finalPayload.put("systemMessageType", resp.getSystemMessageType());
                if (resp.getMeta() != null) {
                    finalPayload.put("meta", resp.getMeta());
                }
                writer.write(mapper.writeValueAsString(finalPayload) + "\n");
                writer.flush();

                com.xs.ai.aiheathagent.conversation.expose.SystemStateSnapshot snapshot = result.snapshot();
                if (snapshot == null && result.systemMessageType() == com.xs.ai.aiheathagent.conversation.expose.SystemMessageType.CHAT) {
                    snapshot = orchestratorService.buildChatSnapshot(input, result.event());
                }
                if (snapshot != null) {
                    writer.write(mapper.writeValueAsString(Map.of(
                            "type", "snapshot",
                            "snapshot", snapshot
                    )) + "\n");
                    writer.flush();
                }
                if (!onboardingKickoff && result.systemMessageType() == com.xs.ai.aiheathagent.conversation.expose.SystemMessageType.CHAT) {
                    List<String> suggestions = followupSuggestionService.suggest(request.getSessionId(), uid, request.getMessage(), resp.getReply());
                    writer.write(mapper.writeValueAsString(Map.of(
                            "type", "suggestions",
                            "suggestions", suggestions == null ? List.of() : suggestions
                    )) + "\n");
                    writer.flush();
                }
            } catch (Exception ex) {
                writer.write(mapper.writeValueAsString(Map.of(
                        "type", "final",
                        "reply", "我这会儿有点忙，但我在。你把目标和今天吃的东西发我，我先给你一版建议。",
                        "systemMessageType", com.xs.ai.aiheathagent.conversation.expose.SystemMessageType.CHAT
                )) + "\n");
                writer.flush();
                writer.write(mapper.writeValueAsString(Map.of(
                        "type", "suggestions",
                        "suggestions", List.of("我想减脂，怎么安排", "我今天吃了这些…", "身高体重是…")
                )) + "\n");
                writer.flush();
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/x-ndjson"));
        headers.setCacheControl("no-store");
        headers.set("X-Accel-Buffering", "no");
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private boolean isOnboardingKickoff(String sessionId, String message) {
        String sid = String.valueOf(sessionId == null ? "" : sessionId).trim();
        if (sid.isEmpty() || !sid.startsWith(ONBOARDING_SESSION_PREFIX)) return false;
        return ONBOARDING_KICKOFF_TOKEN.equals(String.valueOf(message == null ? "" : message).trim());
    }

    private Map<String, Object> buildOnboardingKickoffMeta(Long userId) {
        long now = System.currentTimeMillis();
        String timeText = java.time.LocalTime.now(ZoneId.of("Asia/Shanghai")).withNano(0).toString();
        String createdAt = java.time.LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0).toString();

        Map<String, Object> gate = new HashMap<>();
        String gateDecisionKey = "onboarding_gate:" + userId;
        gate.put("id", "sys_" + gateDecisionKey);
        gate.put("systemMessageType", com.xs.ai.aiheathagent.conversation.expose.SystemMessageType.ONBOARDING_GATE.name());
        gate.put("ts", now + 2);
        gate.put("createdAt", createdAt);
        gate.put("timeText", timeText);
        gate.put("decisionKey", gateDecisionKey);
        gate.put("statusPhase", "PENDING");
        gate.put("statusText", "待开启");
        gate.put("text", "为了让「提醒 / 日终总结」能自动履约，需要先开启服务器侧能力开关。\n这不是消息通知开关；你随时也可以去【我的-设置】修改。");

        Map<String, Object> photo = new HashMap<>();
        photo.put("title", "拍照识别记录");
        photo.put("desc", "拍一张你吃的/菜单，我来识别并帮你记录。");
        photo.put("example", "去拍照");
        photo.put("action", "photo");

        Map<String, Object> reminder = new HashMap<>();
        reminder.put("title", "到点提醒");
        reminder.put("desc", "你说一句话，我帮你安排提醒（喝水/用药/复训）。");
        reminder.put("example", "每天 8 点提醒我喝水");
        reminder.put("action", "fill");

        Map<String, Object> summary = new HashMap<>();
        summary.put("title", "日终复盘");
        summary.put("desc", "晚上我会根据记录，给你一份简短复盘。");
        summary.put("example", "帮我总结今天");
        summary.put("action", "fill");

        Map<String, Object> feature = new HashMap<>();
        String featureDecisionKey = "feature_guide:" + userId;
        feature.put("id", "sys_" + featureDecisionKey);
        feature.put("systemMessageType", com.xs.ai.aiheathagent.conversation.expose.SystemMessageType.FEATURE_GUIDE.name());
        feature.put("ts", now + 3);
        feature.put("createdAt", createdAt);
        feature.put("timeText", timeText);
        feature.put("decisionKey", featureDecisionKey);
        feature.put("features", List.of(photo, reminder, summary));

        Map<String, Object> meta = new HashMap<>();
        meta.put("systemItems", List.of(gate, feature));
        meta.put("hideQuickReplies", true);
        return meta;
    }

    private void tryUpdateEventMeta(Long eventId, HttpServletRequest request, String sessionId) {
        if (eventId == null || request == null) return;
        try {
            var rec = userEventRecordRepository.findById(eventId).orElse(null);
            if (rec == null) return;
            if (rec.getSessionId() == null || rec.getSessionId().isBlank()) {
                String sid = String.valueOf(sessionId == null ? "" : sessionId).trim();
                if (!sid.isEmpty()) rec.setSessionId(sid);
            }
            String channel = reqChannel(request);
            if (!channel.isEmpty()) rec.setChannel(channel);
            String appVersion = reqAppVersion(request);
            if (!appVersion.isEmpty()) rec.setAppVersion(appVersion);
            String scene = reqScene(request);
            if (!scene.isEmpty()) rec.setScene(scene);
            String ip = reqIp(request);
            if (!ip.isEmpty()) rec.setIp(ip);
            String ua = reqUserAgent(request);
            if (!ua.isEmpty()) rec.setUserAgent(ua);
            userEventRecordRepository.save(rec);
        } catch (Exception ignored) {
        }
    }

    @GetMapping({"/api/chat/history", "/api/chat/history/", "/api/api/chat/history", "/api/api/chat/history/", "/chat/history", "/chat/history/"})
    public ResponseEntity<ApiResp<Map<String, Object>>> history(
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "before", required = false) String before,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit
    ) {
        var user = current();
        Long userId = user.getId();

        Long afterId = null;
        try {
            String c = String.valueOf(cursor == null ? "" : cursor).trim();
            if (!c.isEmpty()) afterId = Long.parseLong(c);
        } catch (Exception ignored) {}

        Long beforeId = null;
        try {
            String c = String.valueOf(before == null ? "" : before).trim();
            if (!c.isEmpty()) beforeId = Long.parseLong(c);
        } catch (Exception ignored) {}

        int safeLimit = Math.max(1, Math.min(limit, 200));

        List<ConversationRecord> list;
        if (beforeId != null) {
            list = new ArrayList<>(conversationRepository
                    .findBySessionIdAndUserIdAndIdLessThanOrderByIdDesc(sessionId, userId, beforeId, PageRequest.of(0, safeLimit))
                    .getContent());
            Collections.reverse(list);
        } else if (afterId != null) {
            list = conversationRepository.findBySessionIdAndUserIdAndIdGreaterThanOrderByIdAsc(sessionId, userId, afterId);
            if (list.size() > safeLimit) list = list.subList(0, safeLimit);
        } else {
            list = new ArrayList<>(conversationRepository
                    .findBySessionIdAndUserIdOrderByIdDesc(sessionId, userId, PageRequest.of(0, safeLimit))
                    .getContent());
            Collections.reverse(list);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        String nextCursor = "";
        Map<Long, com.xs.ai.aiheathagent.entity.SystemMessageMetaRecord> metaMap = new HashMap<>();
        try {
            List<Long> ids = new ArrayList<>();
            for (ConversationRecord r : list) {
                if (r != null && r.getId() != null) ids.add(r.getId());
            }
            for (var m : systemMessageMetaRecordRepository.findByConversationRecordIdIn(ids)) {
                metaMap.put(m.getConversationRecordId(), m);
            }
        } catch (Exception ignored) {
        }
        for (ConversationRecord r : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("sessionId", r.getSessionId());
            item.put("messageType", r.getMessageType() == null ? "" : r.getMessageType().name());
            item.put("role", r.getMessageType() == ConversationRecord.MessageType.USER ? "user" : "assistant");
            item.put("content", r.getContent());
            item.put("timestamp", r.getTimestamp() == null ? null : r.getTimestamp().toString());
            var meta = r.getId() == null ? null : metaMap.get(r.getId());
            item.put("systemMessageType", meta == null || meta.getSystemMessageType() == null ? "" : meta.getSystemMessageType().name());
            item.put("explanationCode", meta == null || meta.getExplanationCode() == null ? "" : meta.getExplanationCode().name());
            item.put("explanationArgs", meta == null ? null : meta.getExplanationArgs());
            item.put("decisionKey", meta == null ? "" : String.valueOf(meta.getDecisionKey() == null ? "" : meta.getDecisionKey()));
            item.put("timeSlot", meta == null ? "" : String.valueOf(meta.getTimeSlot() == null ? "" : meta.getTimeSlot()));
            item.put("subject", meta == null ? "" : String.valueOf(meta.getSubject() == null ? "" : meta.getSubject()));
            items.add(item);
            nextCursor = r.getId() == null ? nextCursor : String.valueOf(r.getId());
        }

        if (beforeId != null && !list.isEmpty()) {
            nextCursor = String.valueOf(list.get(0).getId());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("nextCursor", nextCursor);
        return ResponseEntity.ok(ApiResp.ok(data));
    }

    @GetMapping({"/api/inbox/pull", "/api/inbox/pull/", "/api/api/inbox/pull", "/api/api/inbox/pull/", "/inbox/pull", "/inbox/pull/"})
    public ResponseEntity<ApiResp<Map<String, Object>>> pullInbox(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        var user = current();
        Long userId = user.getId();
        String inboxSessionId = "inbox_" + userId;

        long afterId = 0L;
        try {
            String c = String.valueOf(cursor == null ? "" : cursor).trim();
            if (!c.isEmpty()) afterId = Long.parseLong(c);
        } catch (Exception ignored) {}

        int safeLimit = Math.max(1, Math.min(limit, 100));

        List<ConversationRecord> list;
        if (afterId <= 0L) {
            list = new ArrayList<>(conversationRepository
                    .findBySessionIdAndUserIdOrderByIdDesc(inboxSessionId, userId, PageRequest.of(0, safeLimit))
                    .getContent());
            Collections.reverse(list);
        } else {
            list = conversationRepository.findBySessionIdAndUserIdAndIdGreaterThanOrderByIdAsc(inboxSessionId, userId, afterId);
            if (list.size() > safeLimit) list = list.subList(0, safeLimit);
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        String nextCursor = "";
        Map<Long, com.xs.ai.aiheathagent.entity.SystemMessageMetaRecord> metaMap = new HashMap<>();
        try {
            List<Long> ids = new ArrayList<>();
            for (ConversationRecord r : list) {
                if (r != null && r.getId() != null) ids.add(r.getId());
            }
            for (var m : systemMessageMetaRecordRepository.findByConversationRecordIdIn(ids)) {
                metaMap.put(m.getConversationRecordId(), m);
            }
        } catch (Exception ignored) {
        }
        for (ConversationRecord r : list) {
            Map<String, Object> m = new HashMap<>();
            String serverId = r.getId() == null ? "" : String.valueOf(r.getId());
            m.put("serverMessageId", serverId);
            m.put("dedupeKey", serverId);
            m.put("text", r.getContent());
            Long ts = null;
            if (r.getTimestamp() != null) {
                ts = r.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            m.put("ts", ts);
            m.put("createdAt", r.getTimestamp() == null ? null : r.getTimestamp().toString());
            m.put("userName", "");
            m.put("type", "text");
            m.put("source", "server");
            m.put("deepLink", "/pages/aiAgent/aiAgent");
            var meta = r.getId() == null ? null : metaMap.get(r.getId());
            m.put("systemMessageType", meta == null || meta.getSystemMessageType() == null ? "" : meta.getSystemMessageType().name());
            m.put("explanationCode", meta == null || meta.getExplanationCode() == null ? "" : meta.getExplanationCode().name());
            m.put("explanationArgs", meta == null ? null : meta.getExplanationArgs());
            m.put("decisionKey", meta == null ? "" : String.valueOf(meta.getDecisionKey() == null ? "" : meta.getDecisionKey()));
            m.put("timeSlot", meta == null ? "" : String.valueOf(meta.getTimeSlot() == null ? "" : meta.getTimeSlot()));
            m.put("subject", meta == null ? "" : String.valueOf(meta.getSubject() == null ? "" : meta.getSubject()));
            messages.add(m);
            nextCursor = r.getId() == null ? nextCursor : String.valueOf(r.getId());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("messages", messages);
        data.put("nextCursor", nextCursor);
        data.put("ackToken", "");
        return ResponseEntity.ok(ApiResp.ok(data));
    }

    public static class InboxAckReq {
        private String ackToken;
        private List<String> ids;

        public String getAckToken() { return ackToken; }
        public void setAckToken(String ackToken) { this.ackToken = ackToken; }
        public List<String> getIds() { return ids; }
        public void setIds(List<String> ids) { this.ids = ids; }
    }

    @PostMapping({"/api/inbox/ack", "/api/inbox/ack/", "/api/api/inbox/ack", "/api/api/inbox/ack/", "/inbox/ack", "/inbox/ack/"})
    public ResponseEntity<ApiResp<Map<String, Object>>> ackInbox(@RequestBody(required = false) InboxAckReq req) {
        var user = current();
        Long userId = user.getId();
        if (req != null && req.getIds() != null) {
            for (String idStr : req.getIds()) {
                try {
                    Long msgId = Long.parseLong(String.valueOf(idStr == null ? "" : idStr).trim());
                    if (inboxAckRecordRepository.existsByUserIdAndMessageId(userId, msgId)) {
                        continue;
                    }
                    InboxAckRecord rec = new InboxAckRecord();
                    rec.setUserId(userId);
                    rec.setMessageId(msgId);
                    inboxAckRecordRepository.save(rec);
                } catch (Exception ignored) {
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        return ResponseEntity.ok(ApiResp.ok(data));
    }
}
