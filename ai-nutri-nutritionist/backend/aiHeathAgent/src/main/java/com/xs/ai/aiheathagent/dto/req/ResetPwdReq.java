package com.xs.ai.aiheathagent.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPwdReq {
    @NotBlank
    private String account;     // 手机/邮箱/uid 皆可
    @NotBlank private String code;        // 验证码
    @NotBlank @Size(min=6,max=64) private String newPassword;

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
