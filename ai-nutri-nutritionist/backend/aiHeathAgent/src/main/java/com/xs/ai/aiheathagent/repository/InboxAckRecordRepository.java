package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.InboxAckRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxAckRecordRepository extends JpaRepository<InboxAckRecord, Long> {
    boolean existsByUserIdAndMessageId(Long userId, Long messageId);
}

