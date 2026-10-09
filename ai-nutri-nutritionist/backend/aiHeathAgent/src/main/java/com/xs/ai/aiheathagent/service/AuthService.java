package com.xs.ai.aiheathagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.xs.ai.aiheathagent.dto.resp.TokenPair;
import com.xs.ai.aiheathagent.entity.LoginRecord;
import com.xs.ai.aiheathagent.entity.RefreshToken;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.entity.VerificationCode;
import com.xs.ai.aiheathagent.repo.LoginRecordRepo;
import com.xs.ai.aiheathagent.repo.RefreshTokenRepo;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repo.VerificationCodeRepo;
import com.xs.ai.aiheathagent.security.JwtUtil;
import com.xs.ai.aiheathagent.util.AccountUtil;
import com.xs.ai.aiheathagent.util.Ids;
import com.xs.ai.aiheathagent.conversation.service.ProactiveTouchService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final RefreshTokenRepo rtRepo;
    private final VerificationCodeRepo vRepo;
    private final LoginRecordRepo loginRecordRepo;
    private final JwtUtil jwt;
    private final PasswordEncoder encoder;
    private final ProactiveTouchService proactiveTouchService;

    @org.springframework.beans.factory.annotation.Value("${wechat.appid:}")
    private String wxAppId;

    @org.springframework.beans.factory.annotation.Value("${wechat.secret:}")
    private String wxSecret;

    public AuthService(UserRepo userRepo,
                       RefreshTokenRepo rtRepo,
                       VerificationCodeRepo vRepo,
                       LoginRecordRepo loginRecordRepo,
                       JwtUtil jwt,
                       PasswordEncoder encoder,
                       ProactiveTouchService proactiveTouchService) {
        this.userRepo = userRepo;
        this.rtRepo = rtRepo;
        this.vRepo = vRepo;
        this.loginRecordRepo = loginRecordRepo;
        this.jwt = jwt;
        this.encoder = encoder;
        this.proactiveTouchService = proactiveTouchService;
    }

    @Transactional
    public Map<String, Object> register(String account, String code, String password, String nickname) {
        VerificationCode v = vRepo.findTopByTargetAndSceneAndUsedAtIsNullOrderByIdDesc(account, "register")
                .orElseThrow(() -> new BizEx(ErrorCode.BIZ_ERROR, "验证码无效或未获取"));
        if (v.getExpireAt().isBefore(LocalDateTime.now()) || !v.getCode().equals(code)) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "验证码无效或已过期");
        }
        v.setUsedAt(LocalDateTime.now());
        vRepo.save(v);

        userRepo.findByPhone(account).ifPresent(u -> { throw new BizEx(ErrorCode.BIZ_ERROR, "手机号已注册"); });
        userRepo.findByEmail(account).ifPresent(u -> { throw new BizEx(ErrorCode.BIZ_ERROR, "邮箱已注册"); });

        User u = new User();
        u.setUid(Ids.uid());
        if (AccountUtil.isEmail(account)) u.setEmail(account); else u.setPhone(account);
        u.setNickname(nickname == null ? "新用户" : nickname);
        u.setPasswordHash(encoder.encode(password));
        u.setRole("USER");
        u.setStatus(1);
        u.setCreatedAt(LocalDateTime.now());
        u.setLastLoginAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);

        TokenPair tp = issueTokens(u);
        try { proactiveTouchService.pushOnboardingIfNeeded(u, "新用户注册"); } catch (Exception ignored) {}
        return buildAuthResp(u, tp);
    }

    public Map<String, Object> passwordLogin(String account, String password, String ip, String userAgent, String channel, String appVersion, String scene, String sessionId) {
        String acc = String.valueOf(account == null ? "" : account).trim();
        Optional<User> ou = AccountUtil.isEmail(acc) ? userRepo.findByEmail(acc) : userRepo.findByPhone(acc);
        if (ou.isEmpty() && !acc.isEmpty()) {
            ou = userRepo.findByUid(acc);
        }
        User u = ou.orElseThrow(() -> new BizEx(ErrorCode.UNAUTHORIZED, "账号或密码错误"));

        boolean firstLogin = u.getLastLoginAt() == null;
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BizEx(ErrorCode.FORBIDDEN, "账号已禁用");
        }
        String hash = String.valueOf(u.getPasswordHash() == null ? "" : u.getPasswordHash()).trim();
        if (hash.isEmpty() || !encoder.matches(String.valueOf(password == null ? "" : password), hash)) {
            throw new BizEx(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        if (u.getUid() == null || u.getUid().isBlank()) {
            u.setUid(Ids.uid());
        }

        u.setLastLoginAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);

        TokenPair tp = issueTokens(u);
        trySaveLoginRecord(u, "password", ip, userAgent, channel, appVersion, scene, sessionId, tp == null ? "" : tp.getRefreshToken());
        if (firstLogin) {
            try { proactiveTouchService.pushOnboardingIfNeeded(u, "首次登录"); } catch (Exception ignored) {}
        }
        return buildAuthResp(u, tp);
    }

    public Map<String, Object> smsLogin(String phone, String code, String ip, String userAgent, String channel, String appVersion, String scene, String sessionId) {
        VerificationCode v = vRepo.findTopByTargetAndSceneAndUsedAtIsNullOrderByIdDesc(phone, "login")
                .orElseThrow(() -> new BizEx(ErrorCode.BIZ_ERROR, "验证码无效或未获取"));
        if (v.getExpireAt().isBefore(LocalDateTime.now()) || !v.getCode().equals(code)) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "验证码无效或已过期");
        }
        v.setUsedAt(LocalDateTime.now());
        vRepo.save(v);

        final boolean[] created = {false};
        User u = userRepo.findByPhone(phone).orElseGet(() -> {
            created[0] = true;
            User nu = new User();
            nu.setUid(Ids.uid());
            nu.setPhone(phone);
            nu.setNickname("新用户");
            nu.setRole("USER");
            nu.setStatus(1);
            nu.setCreatedAt(LocalDateTime.now());
            nu.setLastLoginAt(LocalDateTime.now());
            nu.setUpdatedAt(LocalDateTime.now());
            return userRepo.save(nu);
        });

        if (u.getUid() == null || u.getUid().isBlank()) {
            u.setUid(Ids.uid());
        }
        u.setLastLoginAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);

        if (created[0]) {
            try { proactiveTouchService.pushOnboardingIfNeeded(u, "短信登录"); } catch (Exception ignored) {}
        }

        TokenPair tp = issueTokens(u);
        trySaveLoginRecord(u, "sms", ip, userAgent, channel, appVersion, scene, sessionId, tp == null ? "" : tp.getRefreshToken());
        return buildAuthResp(u, tp);
    }

    public Map<String, Object> wechatLogin(String code, String nickname, String avatarUrl, String ip, String userAgent, String channel, String appVersion, String scene, String sessionId) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + wxAppId +
                "&secret=" + wxSecret + "&js_code=" + code + "&grant_type=authorization_code";

        RestTemplate restTemplate = new RestTemplate();
        String resp = restTemplate.getForObject(url, String.class);

        String openid = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resp);
            if (root.has("errcode") && root.get("errcode").asInt() != 0) {
                throw new BizEx(ErrorCode.BIZ_ERROR, "微信登录失败: " + (root.has("errmsg") ? root.get("errmsg").asText() : "未知错误"));
            }
            if (root.has("openid")) {
                openid = root.get("openid").asText();
            }
        } catch (Exception e) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "微信授权解析失败");
        }

        if (openid == null) throw new BizEx(ErrorCode.BIZ_ERROR, "无法获取OpenID");

        final String finalOpenid = openid;
        final boolean[] created = {false};
        User u = userRepo.findByOpenid(finalOpenid).orElseGet(() -> {
            created[0] = true;
            User newUser = new User();
            newUser.setUid(Ids.uid());
            newUser.setOpenid(finalOpenid);
            newUser.setNickname(nickname != null ? nickname : "微信用户");
            newUser.setAvatarUrl(avatarUrl);
            newUser.setRole("USER");
            newUser.setStatus(1);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setLastLoginAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            return userRepo.save(newUser);
        });

        u.setLastLoginAt(LocalDateTime.now());
        if (nickname != null && !nickname.isBlank()) {
            u.setNickname(nickname);
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            u.setAvatarUrl(avatarUrl);
        }
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);

        TokenPair tp = issueTokens(u);
        trySaveLoginRecord(u, "wechat", ip, userAgent, channel, appVersion, scene, sessionId, tp == null ? "" : tp.getRefreshToken());
        if (created[0]) {
            try { proactiveTouchService.pushOnboardingIfNeeded(u, "微信登录"); } catch (Exception ignored) {}
        }
        return buildAuthResp(u, tp);
    }

    public TokenPair refresh(String refreshToken) {
        RefreshToken r = rtRepo.findByToken(refreshToken)
                .orElseThrow(() -> new BizEx(ErrorCode.UNAUTHORIZED, "无效刷新令牌"));
        if (r.getRevokedAt() != null || r.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizEx(ErrorCode.UNAUTHORIZED, "刷新令牌已失效");
        }
        return issueTokens(r.getUser());
    }

    public void logout(String refreshToken) {
        RefreshToken r = null;
        try {
            r = rtRepo.findByToken(refreshToken).orElse(null);
        } catch (Exception ignored) {
            r = null;
        }
        if (r == null) return;
        LocalDateTime now = LocalDateTime.now();
        try {
            r.setRevokedAt(now);
            rtRepo.save(r);
        } catch (Exception ignored) {
        }
        try {
            LoginRecord rec = loginRecordRepo.findTopByRefreshTokenOrderByIdDesc(refreshToken).orElse(null);
            if (rec == null) return;
            if (rec.getLogoutAt() == null) rec.setLogoutAt(now);
            if (rec.getCreatedAt() != null && rec.getLogoutAt() != null) {
                long sec = java.time.Duration.between(rec.getCreatedAt(), rec.getLogoutAt()).getSeconds();
                rec.setDurationSec(Math.max(0, sec));
            }
            loginRecordRepo.save(rec);
        } catch (Exception ignored) {
        }
    }

    public TokenPair issueTokens(User u) {
        if (u.getUid() == null || u.getUid().isBlank()) {
            u.setUid(Ids.uid());
            u.setUpdatedAt(LocalDateTime.now());
            userRepo.save(u);
        }

        String at = jwt.createAccessToken(u.getUid());
        if (at == null || at.isBlank()) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "无法签发访问令牌");
        }

        RefreshToken rt = new RefreshToken();
        rt.setUser(u);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiresAt(LocalDateTime.now().plusDays(30));
        rt.setCreatedAt(LocalDateTime.now());
        rtRepo.save(rt);

        return new TokenPair(at, rt.getToken());
    }

    private Map<String, Object> buildAuthResp(User u, TokenPair tp) {
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("uid", u.getUid());
        userMap.put("nickname", u.getNickname());
        userMap.put("avatarUrl", u.getAvatarUrl());
        userMap.put("role", u.getRole());
        userMap.put("id", u.getUid());
        userMap.put("avatar", u.getAvatarUrl());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("user", userMap);
        resp.put("accessToken", tp.getAccessToken());
        resp.put("refreshToken", tp.getRefreshToken());
        return resp;
    }

    private void trySaveLoginRecord(User u, String method, String ip, String userAgent, String channel, String appVersion, String scene, String sessionId, String refreshToken) {
        if (u == null || u.getId() == null) return;
        try {
            LoginRecord rec = new LoginRecord();
            rec.setUser(u);
            rec.setMethod(String.valueOf(method == null ? "" : method).trim());
            rec.setIp(normalize(ip, 64));
            rec.setUserAgent(normalize(userAgent, 512));
            rec.setChannel(normalize(channel, 64));
            rec.setAppVersion(normalize(appVersion, 64));
            rec.setScene(normalize(scene, 64));
            rec.setSessionId(normalize(sessionId, 128));
            rec.setRefreshToken(normalize(refreshToken, 128));
            rec.setSuccess(true);
            rec.setErrorMsg(null);
            rec.setCreatedAt(LocalDateTime.now());
            loginRecordRepo.save(rec);
        } catch (Exception ignored) {
        }
    }

    private static String normalize(String v, int maxLen) {
        String s = String.valueOf(v == null ? "" : v).trim();
        if (s.isEmpty()) return "";
        int limit = Math.max(1, Math.min(maxLen, 2048));
        if (s.length() <= limit) return s;
        return s.substring(0, limit);
    }

    @Transactional
    public void updatePasswordByAccount(String account, String newPassword){
        User u = findUserByAnyAccount(account)
                .orElseThrow(() -> new BizEx(ErrorCode.BIZ_ERROR, "账号不存在"));
        u.setPasswordHash(encoder.encode(newPassword));
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);
    }

    public Optional<User> findUserByAnyAccount(String account){
        if (AccountUtil.isEmail(account)) return userRepo.findByEmail(account);
        if (AccountUtil.isPhone(account)) return userRepo.findByPhone(account);
        return userRepo.findByUid(account);
    }
}
