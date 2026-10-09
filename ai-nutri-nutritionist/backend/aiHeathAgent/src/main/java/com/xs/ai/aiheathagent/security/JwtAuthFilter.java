package com.xs.ai.aiheathagent.security;
import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.admin.repo.AdminUserRepo;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;


@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwt;
    private final UserRepo userRepo;
    private final AdminUserRepo adminUserRepo;
    private final long userCacheTtlMs;
    private final int userCacheMaxSize;
    private final Map<String, CacheEntry> userCache = new ConcurrentHashMap<>();
    private final Map<String, AdminCacheEntry> adminCache = new ConcurrentHashMap<>();

    public JwtAuthFilter(
            JwtUtil jwt,
            UserRepo userRepo,
            AdminUserRepo adminUserRepo,
            @Value("${jwt.user-cache-ttl-ms:60000}") long userCacheTtlMs,
            @Value("${jwt.user-cache-max-size:10000}") int userCacheMaxSize
    ) {
        this.jwt = jwt;
        this.userRepo = userRepo;
        this.adminUserRepo = adminUserRepo;
        this.userCacheTtlMs = Math.max(0, userCacheTtlMs);
        this.userCacheMaxSize = Math.max(0, userCacheMaxSize);
    }


    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h!=null && h.startsWith("Bearer ")){
            String token = h.substring(7);
            try {
                String subject = jwt.parseSubject(token);
                String sub = String.valueOf(subject == null ? "" : subject).trim();
                if (sub.startsWith("admin:")) {
                    String adminUid = sub.substring("admin:".length()).trim();
                    AdminUser principal = resolveAdminFromCache(adminUid);
                    if (principal != null && (principal.getStatus() == null || principal.getStatus() != 0)) {
                        List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_USER")
                        );
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } else {
                    User principal = resolveUserFromCache(sub);
                    if (principal != null) {
                        if (principal.getStatus() == null || principal.getStatus() != 0) {
                            String role = String.valueOf(principal.getRole() == null ? "" : principal.getRole()).trim().toUpperCase();
                            List<SimpleGrantedAuthority> authorities = "ADMIN".equals(role)
                                    ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                                    : List.of(new SimpleGrantedAuthority("ROLE_USER"));
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        chain.doFilter(req, res);
    }

    private User resolveUserFromCache(String uid) {
        String key = String.valueOf(uid == null ? "" : uid).trim();
        if (key.isEmpty()) return null;
        long now = System.currentTimeMillis();

        CacheEntry hit = userCache.get(key);
        if (hit != null && hit.user != null && hit.expiresAtMs >= now) {
            return hit.user;
        }

        User user = userRepo.findByUid(key).orElse(null);
        if (userCacheMaxSize > 0 && userCache.size() > userCacheMaxSize) {
            userCache.clear();
        }
        if (userCacheTtlMs <= 0) {
            if (user == null) userCache.remove(key);
            return user;
        }
        if (user == null) {
            userCache.remove(key);
            return null;
        }
        userCache.put(key, new CacheEntry(user, now + userCacheTtlMs));
        return user;
    }

    private AdminUser resolveAdminFromCache(String uid) {
        String key = String.valueOf(uid == null ? "" : uid).trim();
        if (key.isEmpty()) return null;
        long now = System.currentTimeMillis();

        AdminCacheEntry hit = adminCache.get(key);
        if (hit != null && hit.user != null && hit.expiresAtMs >= now) {
            return hit.user;
        }

        AdminUser user = adminUserRepo.findByUid(key).orElse(null);
        if (userCacheMaxSize > 0 && adminCache.size() > userCacheMaxSize) {
            adminCache.clear();
        }
        if (userCacheTtlMs <= 0) {
            if (user == null) adminCache.remove(key);
            return user;
        }
        if (user == null) {
            adminCache.remove(key);
            return null;
        }
        adminCache.put(key, new AdminCacheEntry(user, now + userCacheTtlMs));
        return user;
    }

    private record CacheEntry(User user, long expiresAtMs) {}
    private record AdminCacheEntry(AdminUser user, long expiresAtMs) {}
}
