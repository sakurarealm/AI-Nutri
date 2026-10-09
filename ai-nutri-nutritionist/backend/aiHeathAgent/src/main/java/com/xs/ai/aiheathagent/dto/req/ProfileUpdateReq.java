package com.xs.ai.aiheathagent.dto.req;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;


@Data
public class ProfileUpdateReq {
    private BigDecimal heightCm; private BigDecimal targetWeightKg; private BigDecimal lastWeightKg; private String sex; private LocalDate birthday;

    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getTargetWeightKg() { return targetWeightKg; }
    public void setTargetWeightKg(BigDecimal targetWeightKg) { this.targetWeightKg = targetWeightKg; }
    public BigDecimal getLastWeightKg() { return lastWeightKg; }
    public void setLastWeightKg(BigDecimal lastWeightKg) { this.lastWeightKg = lastWeightKg; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
}
