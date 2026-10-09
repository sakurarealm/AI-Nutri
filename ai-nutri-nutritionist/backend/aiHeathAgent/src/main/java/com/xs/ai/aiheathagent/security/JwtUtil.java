package com.xs.ai.aiheathagent.security;

import com.xs.ai.aiheathagent.config.JwtProps;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    private final JwtProps props;
    private final byte[] keyBytes;

    public JwtUtil(JwtProps props) {
        this.props = props;
        this.keyBytes = deriveKeyBytes(props.getSecret());
    }

    private byte[] deriveKeyBytes(String secret) {
        try {
            // 优先尝试 Base64
            byte[] maybe = Base64.getDecoder().decode(secret);
            if (maybe.length >= 32) return maybe;
        } catch (IllegalArgumentException ignore) {}
        try {
            // 退化：对明文做 SHA-256，保证 >=32 字节（便于开发不至于崩）
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 最差情况：直接字节数组（可能抛 WeakKeyException）
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String createAccessToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(props.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getExpiration())))
                .signWith(Keys.hmacShaKeyFor(keyBytes))
                .compact();
    }

    public String parseSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(keyBytes))
                .requireIssuer(props.getIssuer())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
