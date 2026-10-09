package com.xs.ai.aiheathagent.entity;

import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import com.xs.ai.aiheathagent.conversation.expose.SystemMessageType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_message_meta_records", indexes = {
        @Index(name = "idx_sysmsg_meta_user_time", columnList = "userId,createdAt"),
        @Index(name = "idx_sysmsg_meta_conv", columnList = "conversationRecordId", unique = true)
})
@Data
public class SystemMessageMetaRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long conversationRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SystemMessageType systemMessageType;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private ExplanationCode explanationCode;

    @Column(columnDefinition = "TEXT")
    private String explanationArgs;

    private String decisionKey;

    private String timeSlot;

    private String subject;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getConversationRecordId() { return conversationRecordId; }
    public void setConversationRecordId(Long conversationRecordId) { this.conversationRecordId = conversationRecordId; }
    public SystemMessageType getSystemMessageType() { return systemMessageType; }
    public void setSystemMessageType(SystemMessageType systemMessageType) { this.systemMessageType = systemMessageType; }
    public ExplanationCode getExplanationCode() { return explanationCode; }
    public void setExplanationCode(ExplanationCode explanationCode) { this.explanationCode = explanationCode; }
    public String getExplanationArgs() { return explanationArgs; }
    public void setExplanationArgs(String explanationArgs) { this.explanationArgs = explanationArgs; }
    public String getDecisionKey() { return decisionKey; }
    public void setDecisionKey(String decisionKey) { this.decisionKey = decisionKey; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
