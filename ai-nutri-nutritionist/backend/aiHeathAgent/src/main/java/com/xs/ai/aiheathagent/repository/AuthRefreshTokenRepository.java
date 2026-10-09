package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {
    AuthRefreshToken findByToken(String token);
}