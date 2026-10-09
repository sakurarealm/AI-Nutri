package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.UserVipSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserVipSubscriptionRepo extends JpaRepository<UserVipSubscription, Long> {
    Optional<UserVipSubscription> findByUserId(Long userId);
}

