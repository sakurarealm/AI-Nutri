package com.xs.ai.aiheathagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProps {
    /** 任意字符串；建议用环境变量注入强随机密钥 */
    private String secret = "REPLACE_WITH_BASE64_32B_MIN_KEY";
    /** 发行者（issuer） */
    private String issuer = "aiHeathAgent";
    /** 访问令牌过期秒数（对齐 yml 的 jwt.expiration） */
    private long expiration = 86400;
}
