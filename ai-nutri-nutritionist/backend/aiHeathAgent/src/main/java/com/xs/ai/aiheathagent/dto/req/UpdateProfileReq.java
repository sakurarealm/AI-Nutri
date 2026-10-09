package com.xs.ai.aiheathagent.dto.req;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 用户资料更新请求体
 * 对齐后端 UserService.updateProfile 的参数命名：
 * nickname, avatarUrl, gender, birthDate, heightCm, weightKg
 */
@Data
public class UpdateProfileReq {
    private String nickname;
    private String avatarUrl; // 前端 userInfo.avatar 映射为 avatarUrl
    private Integer gender;   // 0/1/2
    private String birthDate; // yyyy-MM-dd
    private BigDecimal heightCm;
    private BigDecimal weightKg;
}
