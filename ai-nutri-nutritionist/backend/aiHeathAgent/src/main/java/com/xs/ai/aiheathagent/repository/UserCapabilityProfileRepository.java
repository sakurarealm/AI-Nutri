package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.UserCapabilityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCapabilityProfileRepository extends JpaRepository<UserCapabilityProfile, Long> {
    Optional<UserCapabilityProfile> findByUserId(Long userId);
}

