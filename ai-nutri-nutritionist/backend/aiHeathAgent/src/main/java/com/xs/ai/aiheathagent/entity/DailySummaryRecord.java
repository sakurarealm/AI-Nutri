package com.xs.ai.aiheathagent.entity;

import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_summary_records", indexes = {
        @Index(name = "idx_daily_summary_user_date", columnList = "userId,date")
})
@Data
public class DailySummaryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String familyText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String clinicalJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExplanationCode explanationCode = ExplanationCode.SUMMARY_SCHEDULED_DAILY;

    @Column(columnDefinition = "TEXT")
    private String explanationArgs;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getUserText() { return userText; }
    public void setUserText(String userText) { this.userText = userText; }
    public String getFamilyText() { return familyText; }
    public void setFamilyText(String familyText) { this.familyText = familyText; }
    public String getClinicalJson() { return clinicalJson; }
    public void setClinicalJson(String clinicalJson) { this.clinicalJson = clinicalJson; }
    public ExplanationCode getExplanationCode() { return explanationCode; }
    public void setExplanationCode(ExplanationCode explanationCode) { this.explanationCode = explanationCode; }
    public String getExplanationArgs() { return explanationArgs; }
    public void setExplanationArgs(String explanationArgs) { this.explanationArgs = explanationArgs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
