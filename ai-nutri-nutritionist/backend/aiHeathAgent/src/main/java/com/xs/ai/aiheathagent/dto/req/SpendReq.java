package com.xs.ai.aiheathagent.dto.req;


import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;


@Data
public class SpendReq {
    /** 本次扣费的 K币数量，必须 > 0 */
    @NotNull @DecimalMin(value = "0.01", message = "amountKc 必须大于 0")
    private BigDecimal amountKc;


    /** 幂等键：同一个业务请求必须保持唯一（例如 UUID） */
    @NotBlank
    private String idempotentKey;


    private String bizType; // 可选
    private String bizRef; // 可选
    private String memo; // 可选
}