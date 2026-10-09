package com.xs.ai.aiheathagent.dto.req;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class LoginReq {
    @NotBlank private String account; // phone or email
    @NotBlank private String password;
}