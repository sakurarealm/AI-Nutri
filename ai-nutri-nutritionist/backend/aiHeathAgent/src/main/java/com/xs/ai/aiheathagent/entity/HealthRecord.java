package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity @Table(name = "health_records", indexes = {
        @Index(name = "idx_hr_user", columnList = "user_id"),
        @Index(name = "idx_hr_user_type_time", columnList = "user_id,type,recorded_at")
})
@Data
public class HealthRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String type; // weight/diet/sport/sleep/body


    // 通用数值位（视 type 使用）：例如 weight->value1:kg；blood pressure->value1:高压,value2:低压
    @Column(name = "value1") private BigDecimal value1;
    @Column(name = "value2") private BigDecimal value2;
    @Column(name = "unit1") private String unit1;
    @Column(name = "unit2") private String unit2;


    @Column(name = "note", length = 1000) private String note;
    @Column(name = "ext", columnDefinition = "json") private String ext; // 自由扩展：如 diet 菜单、sport 卡路里


    @Column(name = "recorded_at", nullable = false) private LocalDateTime recordedAt; // 记录时间
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getValue1() { return value1; }
    public void setValue1(BigDecimal value1) { this.value1 = value1; }
    public BigDecimal getValue2() { return value2; }
    public void setValue2(BigDecimal value2) { this.value2 = value2; }
    public String getUnit1() { return unit1; }
    public void setUnit1(String unit1) { this.unit1 = unit1; }
    public String getUnit2() { return unit2; }
    public void setUnit2(String unit2) { this.unit2 = unit2; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
