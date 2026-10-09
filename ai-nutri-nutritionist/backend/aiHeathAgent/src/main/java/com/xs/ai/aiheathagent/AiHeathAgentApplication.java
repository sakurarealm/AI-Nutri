package com.xs.ai.aiheathagent;

import com.xs.ai.aiheathagent.config.JwtProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableConfigurationProperties(JwtProps.class)
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class AiHeathAgentApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(AiHeathAgentApplication.class, args);
    }
}
