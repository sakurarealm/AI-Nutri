package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.DecisionResult;
import com.xs.ai.aiheathagent.conversation.model.PlanAction;
import com.xs.ai.aiheathagent.conversation.model.PlanResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PlanGenerateService {
    @Async
    public void trigger(DecisionResult decision) {
        if (decision == null || decision.userId() == null) {
            return;
        }
        generate(decision.userId());
    }

    public PlanResult generate(Long userId) {
        return new PlanResult(userId, LocalDate.now(), "", List.<PlanAction>of());
    }
}
