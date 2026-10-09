package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepo extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findTopByTargetAndSceneAndUsedAtIsNullOrderByIdDesc(String target, String scene);
}