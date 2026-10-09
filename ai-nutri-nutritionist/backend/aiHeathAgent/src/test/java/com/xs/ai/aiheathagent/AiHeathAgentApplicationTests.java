package com.xs.ai.aiheathagent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(properties = {
        "inbox.proactive.enabled=true",
        "inbox.proactive.quiet-start-hour=0",
        "inbox.proactive.quiet-end-hour=0",
        "inbox.proactive.min-gap-minutes=1",
        "inbox.proactive.active-gap-minutes=1",
        "inbox.proactive.max-per-day=10",
        "inbox.proactive.only-login-within-days=365"
})
class AiHeathAgentApplicationTests {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void contextLoads() {
    }

    @Test
    void userVipEndpointMapped() {
        boolean found = requestMappingHandlerMapping.getHandlerMethods()
                .keySet()
                .stream()
                .anyMatch(info -> info.getPatternValues().contains("/api/user/vip"));
        Assertions.assertTrue(found);
    }

    @Test
    void todayPlanEndpointMapped() {
        boolean found = requestMappingHandlerMapping.getHandlerMethods()
                .keySet()
                .stream()
                .anyMatch(info -> info.getPatternValues().contains("/api/plan/today"));
        Assertions.assertTrue(found);
    }
}
