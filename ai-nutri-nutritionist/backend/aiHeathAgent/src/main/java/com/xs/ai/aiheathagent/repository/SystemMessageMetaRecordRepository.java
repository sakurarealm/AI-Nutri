package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.SystemMessageMetaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import com.xs.ai.aiheathagent.conversation.expose.SystemMessageType;

@Repository
public interface SystemMessageMetaRecordRepository extends JpaRepository<SystemMessageMetaRecord, Long> {
    Optional<SystemMessageMetaRecord> findByConversationRecordId(Long conversationRecordId);
    List<SystemMessageMetaRecord> findByConversationRecordIdIn(List<Long> conversationRecordIds);
    Optional<SystemMessageMetaRecord> findTopByUserIdAndSystemMessageTypeOrderByCreatedAtDescIdDesc(Long userId, SystemMessageType systemMessageType);
    long countByUserIdAndSystemMessageTypeAndCreatedAtGreaterThanEqual(Long userId, SystemMessageType systemMessageType, LocalDateTime createdAt);
}
