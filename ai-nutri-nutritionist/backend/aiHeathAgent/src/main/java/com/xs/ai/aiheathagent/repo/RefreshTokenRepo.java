package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    @Modifying
    @Transactional
    @Query("update RefreshToken r set r.revokedAt = CURRENT_TIMESTAMP where r.user.id = :uid")
    void revokeAllByUser(@Param("uid") Long userId);

    void deleteByUser_IdIn(List<Long> userIds);
}
