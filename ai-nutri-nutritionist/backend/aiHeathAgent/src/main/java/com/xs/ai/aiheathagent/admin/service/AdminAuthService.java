package com.xs.ai.aiheathagent.admin.service;

import com.xs.ai.aiheathagent.admin.entity.AdminRefreshToken;
import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.admin.repo.AdminRefreshTokenRepo;
import com.xs.ai.aiheathagent.admin.repo.AdminUserRepo;
import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.xs.ai.aiheathagent.dto.resp.TokenPair;
import com.xs.ai.aiheathagent.security.JwtUtil;
import com.xs.ai.aiheathagent.util.AccountUtil;
import com.xs.ai.aiheathagent.util.Ids;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminAuthService {
    private final AdminUserRepo adminUserRepo;
    private final AdminRefreshTokenRepo adminRefreshTokenRepo;
    private final JwtUtil jwt;
    private final PasswordEncoder encoder;

    public AdminAuthService(AdminUserRepo adminUserRepo, AdminRefreshTokenRepo adminRefreshTokenRepo, JwtUtil jwt, PasswordEncoder encoder) {
        this.adminUserRepo = adminUserRepo;
        this.adminRefreshTokenRepo = adminRefreshTokenRepo;
        this.jwt = jwt;
        this.encoder = encoder;
    }

    @PostConstruct
    public void ensureDefaultAdmin() {
        if (adminUserRepo.findByUsername("admin").isPresent()) return;

        AdminUser admin = new AdminUser();
        admin.setUid(Ids.uid());
        admin.setUsername("admin");
        admin.setNickname("管理员");
        admin.setAvatarUrl("");
        admin.setRole("ADMIN");
        admin.setRoleLevel(1);
        admin.setBizLine("all");
        admin.setStatus(1);
        admin.setPasswordHash(encoder.encode("admin"));
        admin.setFailedLoginCount(0);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(admin);
    }

    @Transactional
    public TokenPair login(String account, String password) {
        AdminUser admin = findAdminByAccount(account)
                .orElseThrow(() -> new BizEx(ErrorCode.UNAUTHORIZED, "账号或密码错误"));
        if (String.valueOf(admin.getRole() == null ? "" : admin.getRole()).trim().isEmpty()) {
            admin.setRole("ADMIN");
        }
        if (admin.getRoleLevel() == null) {
            String biz = String.valueOf(admin.getBizLine() == null ? "" : admin.getBizLine()).trim();
            admin.setRoleLevel(biz.isEmpty() || "all".equalsIgnoreCase(biz) ? 1 : 2);
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BizEx(ErrorCode.FORBIDDEN, "账号已禁用");
        }
        if (admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BizEx(ErrorCode.FORBIDDEN, "账号已锁定，请稍后再试");
        }
        String hash = String.valueOf(admin.getPasswordHash() == null ? "" : admin.getPasswordHash()).trim();
        if (hash.isEmpty() || !encoder.matches(String.valueOf(password == null ? "" : password), hash)) {
            int failed = (admin.getFailedLoginCount() == null ? 0 : admin.getFailedLoginCount()) + 1;
            admin.setFailedLoginCount(failed);
            if (failed >= 5) {
                admin.setLockedUntil(LocalDateTime.now().plusHours(1));
            }
            if (admin.getCreatedAt() == null) admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            adminUserRepo.save(admin);
            throw new BizEx(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        admin.setFailedLoginCount(0);
        admin.setLockedUntil(null);
        admin.setLastLoginAt(LocalDateTime.now());
        if (admin.getCreatedAt() == null) admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(admin);
        return issueTokens(admin);
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        AdminRefreshToken r = adminRefreshTokenRepo.findByToken(String.valueOf(refreshToken == null ? "" : refreshToken).trim())
                .orElseThrow(() -> new BizEx(ErrorCode.UNAUTHORIZED, "无效刷新令牌"));
        if (r.getRevokedAt() != null || r.getExpiresAt() == null || r.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizEx(ErrorCode.UNAUTHORIZED, "刷新令牌已失效");
        }
        if (r.getAdminUser() == null) {
            throw new BizEx(ErrorCode.UNAUTHORIZED, "无效刷新令牌");
        }
        return issueTokens(r.getAdminUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        String tk = String.valueOf(refreshToken == null ? "" : refreshToken).trim();
        if (tk.isEmpty()) return;
        adminRefreshTokenRepo.findByToken(tk).ifPresent(x -> {
            x.setRevokedAt(LocalDateTime.now());
            adminRefreshTokenRepo.save(x);
        });
    }

    @Transactional
    public TokenPair issueTokens(AdminUser admin) {
        if (admin.getUid() == null || admin.getUid().isBlank()) {
            admin.setUid(Ids.uid());
            if (admin.getCreatedAt() == null) admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            adminUserRepo.save(admin);
        }

        String at = jwt.createAccessToken("admin:" + admin.getUid());
        if (at == null || at.isBlank()) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "无法签发访问令牌");
        }

        AdminRefreshToken rt = new AdminRefreshToken();
        rt.setAdminUser(admin);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiresAt(LocalDateTime.now().plusDays(30));
        rt.setCreatedAt(LocalDateTime.now());
        adminRefreshTokenRepo.save(rt);

        return new TokenPair(at, rt.getToken());
    }

    public Optional<AdminUser> findAdminByAccount(String account) {
        String acc = String.valueOf(account == null ? "" : account).trim();
        if (acc.isEmpty()) return Optional.empty();
        if (AccountUtil.isEmail(acc)) {
            Optional<AdminUser> byEmail = adminUserRepo.findByEmail(acc);
            if (byEmail.isPresent()) return byEmail;
        }
        Optional<AdminUser> byUsername = adminUserRepo.findByUsername(acc);
        if (byUsername.isPresent()) return byUsername;
        Optional<AdminUser> byUid = adminUserRepo.findByUid(acc);
        if (byUid.isPresent()) return byUid;
        return adminUserRepo.findByEmail(acc);
    }
}
