package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_ack_records", indexes = {
        @Index(name = "idx_inbox_ack_user_msg", columnList = "userId,messageId", unique = true)
})
@Data
public class InboxAckRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long messageId;

    @Column(nullable = false)
    private LocalDateTime ackAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public LocalDateTime getAckAt() { return ackAt; }
    public void setAckAt(LocalDateTime ackAt) { this.ackAt = ackAt; }
}
