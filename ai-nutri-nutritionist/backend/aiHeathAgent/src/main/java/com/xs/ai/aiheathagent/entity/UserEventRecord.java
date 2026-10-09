package com.xs.ai.aiheathagent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_event_records")
@Data
public class UserEventRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column(nullable = false)
    private String modality;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topics;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false)
    private boolean suggestOpenCamera;

    @Column(nullable = false)
    private boolean suggestWriteNutrition;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentText;

    private String channel;

    private String appVersion;

    private String scene;

    private String ip;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTopics() { return topics; }
    public void setTopics(String topics) { this.topics = topics; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public boolean isSuggestOpenCamera() { return suggestOpenCamera; }
    public void setSuggestOpenCamera(boolean suggestOpenCamera) { this.suggestOpenCamera = suggestOpenCamera; }
    public boolean isSuggestWriteNutrition() { return suggestWriteNutrition; }
    public void setSuggestWriteNutrition(boolean suggestWriteNutrition) { this.suggestWriteNutrition = suggestWriteNutrition; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
