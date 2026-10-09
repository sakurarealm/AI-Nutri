package com.xs.ai.aiheathagent.dto.req;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class RedeemReq {
    @NotBlank private String code;
}