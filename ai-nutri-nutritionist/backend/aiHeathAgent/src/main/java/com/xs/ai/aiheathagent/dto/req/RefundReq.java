package com.xs.ai.aiheathagent.dto.req;


import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;


@Data
public class RefundReq {
    /** 目标消费单：orderNo 与 spendIdempotentKey 二选一（推荐 orderNo） */
    private String orderNo;
    private String spendIdempotentKey;


    /** 本次退款的 K币数量；如果为空，则表示全额可退款部分 */
    @DecimalMin(value = "0.01", message = "refundAmountKc 必须大于 0")
    private BigDecimal refundAmountKc;


    /** 幂等键（退款级别） */
    @NotBlank
    private String idempotentKey;


    private String memo; // 可选
}