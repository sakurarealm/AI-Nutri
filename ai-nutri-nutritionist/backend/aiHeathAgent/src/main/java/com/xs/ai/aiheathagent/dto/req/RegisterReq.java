package com.xs.ai.aiheathagent.dto.req;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class RegisterReq {
    @NotBlank(message = "账号不能为空")
    private String account; // phone or email
    @NotBlank(message = "验证码不能为空")
    private String code;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String nickname;
}