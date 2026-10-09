package com.xs.ai.aiheathagent.dto.req;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class SmsLoginReq {
    @NotBlank private String phone;
    @NotBlank private String code;
}