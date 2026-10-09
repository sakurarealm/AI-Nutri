package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.conversation.expose.SystemMessageType;
import com.xs.ai.aiheathagent.entity.SystemMessageMetaRecord;
import com.xs.ai.aiheathagent.repository.SystemMessageMetaRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemMessageMetaService {
    private final SystemMessageMetaRecordRepository repository;

    public SystemMessageMetaService(SystemMessageMetaRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void attach(Long userId, Long conversationRecordId, SystemMessageType type, ExplanationCode explanationCode, String explanationArgs) {
        attach(userId, conversationRecordId, type, explanationCode, explanationArgs, null, null, null);
    }

    @Transactional
    public void attach(
            Long userId,
            Long conversationRecordId,
            SystemMessageType type,
            ExplanationCode explanationCode,
            String explanationArgs,
            String decisionKey,
            String timeSlot,
            String subject
    ) {
        if (userId == null || conversationRecordId == null || type == null) {
            return;
        }
        if (repository.findByConversationRecordId(conversationRecordId).isPresent()) {
            return;
        }
        SystemMessageMetaRecord rec = new SystemMessageMetaRecord();
        rec.setUserId(userId);
        rec.setConversationRecordId(conversationRecordId);
        rec.setSystemMessageType(type);
        rec.setExplanationCode(explanationCode);
        rec.setExplanationArgs(explanationArgs);
        rec.setDecisionKey(decisionKey);
        rec.setTimeSlot(timeSlot);
        rec.setSubject(subject);
        repository.save(rec);
    }
}
