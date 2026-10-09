package com.xs.ai.aiheathagent.dto.req;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class CreateOrderReq {
    @NotNull private Long packageId;
    @NotBlank private String payChannel; // wechat/alipay/apple/test
}