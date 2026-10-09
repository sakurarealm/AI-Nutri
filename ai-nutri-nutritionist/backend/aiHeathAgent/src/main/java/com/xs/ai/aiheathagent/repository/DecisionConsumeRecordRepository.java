package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.DecisionConsumeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DecisionConsumeRecordRepository extends JpaRepository<DecisionConsumeRecord, Long> {
    boolean existsByUserIdAndDecisionKey(Long userId, String decisionKey);
}

