package com.xs.ai.aiheathagent.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.time.*;


@Entity @Table(name="login_records") @Data
public class LoginRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="user_id")
    private User user;
    private String method; // password/sms/wechat/alipay/apple
    private String ip;
    private String userAgent;
    private String channel;
    private String appVersion;
    private String scene;
    private String sessionId;
    private String refreshToken;
    private Boolean success;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime logoutAt;
    private Long durationSec;
}
