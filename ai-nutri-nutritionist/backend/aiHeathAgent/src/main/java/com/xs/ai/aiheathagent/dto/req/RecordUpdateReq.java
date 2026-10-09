package com.xs.ai.aiheathagent.dto.req;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class RecordUpdateReq {
    @NotNull private Long id;
    @NotBlank private String type;
    private java.math.BigDecimal value1;
    private java.math.BigDecimal value2;
    private String unit1; private String unit2; private String note; private String ext;
    private java.time.LocalDateTime recordedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public java.math.BigDecimal getValue1() { return value1; }
    public void setValue1(java.math.BigDecimal value1) { this.value1 = value1; }
    public java.math.BigDecimal getValue2() { return value2; }
    public void setValue2(java.math.BigDecimal value2) { this.value2 = value2; }
    public String getUnit1() { return unit1; }
    public void setUnit1(String unit1) { this.unit1 = unit1; }
    public String getUnit2() { return unit2; }
    public void setUnit2(String unit2) { this.unit2 = unit2; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }
    public java.time.LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(java.time.LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
