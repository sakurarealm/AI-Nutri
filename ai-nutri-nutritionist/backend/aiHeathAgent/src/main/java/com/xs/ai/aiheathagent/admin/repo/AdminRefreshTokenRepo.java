package com.xs.ai.aiheathagent.admin.repo;

import com.xs.ai.aiheathagent.admin.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRefreshTokenRepo extends JpaRepository<AdminRefreshToken, Long> {
    Optional<AdminRefreshToken> findByToken(String token);

    void deleteByAdminUser_IdIn(List<Long> adminUserIds);
}
