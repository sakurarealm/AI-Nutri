package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.UserCapabilityChangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCapabilityChangeRecordRepository extends JpaRepository<UserCapabilityChangeRecord, Long> {
    List<UserCapabilityChangeRecord> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}

