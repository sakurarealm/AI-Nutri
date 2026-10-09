package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.math.BigDecimal;


@Data @AllArgsConstructor @NoArgsConstructor
public class MeResp {
    private String id; // 兼容前端 userInfo.id -> 实际为 uid
    private String uid; // 也返回 uid，便于后续使用
    private String nickname;
    private String avatar; // 兼容前端 userInfo.avatar -> 实际为 avatarUrl
    private String avatarUrl; // 同步返回
    private Integer gender;
    private String birthDate; // yyyy-MM-dd
    private BigDecimal height;
    private BigDecimal weight;
}