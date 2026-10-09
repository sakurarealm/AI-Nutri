package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;


@Data @AllArgsConstructor @NoArgsConstructor
public class TokenPair {
    private String accessToken;
    private String refreshToken;
}
