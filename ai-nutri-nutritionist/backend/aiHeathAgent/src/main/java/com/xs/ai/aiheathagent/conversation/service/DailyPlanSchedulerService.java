package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.DecisionActionType;
import com.xs.ai.aiheathagent.conversation.model.DecisionResult;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DailyPlanSchedulerService {
    private final UserRepo userRepo;
    private final HealthDecisionService healthDecisionService;
    private final DailyPlanService dailyPlanService;
    private final int scanPageSize;

    public DailyPlanSchedulerService(
            UserRepo userRepo,
            HealthDecisionService healthDecisionService,
            DailyPlanService dailyPlanService,
            @Value("${dailyplan.scan-page-size:500}") int scanPageSize
    ) {
        this.userRepo = userRepo;
        this.healthDecisionService = healthDecisionService;
        this.dailyPlanService = dailyPlanService;
        this.scanPageSize = Math.max(20, scanPageSize);
    }

    @Scheduled(cron = "${dailyplan.cron:0 30 21 * * *}")
    public void scheduledGenerateDailyPlans() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);
        int page = 0;
        while (true) {
            Page<User> users;
            try {
                users = userRepo.findActiveUsers(PageRequest.of(page, scanPageSize));
            } catch (Exception e) {
                users = Page.empty();
            }
            if (users == null || users.isEmpty()) break;
            for (User u : users.getContent()) {
                try {
                    if (u == null || u.getId() == null) continue;
                    if (u.getStatus() != null && u.getStatus() == 0) continue;

                    DecisionResult d1 = healthDecisionService.decideDailyPlanGeneration(u, today, now);
                    if (d1.actions() != null && d1.actions().contains(DecisionActionType.GENERATE_DAILY_PLAN)) {
                        dailyPlanService.generateAndPersist(u, today);
                    }
                    DecisionResult d2 = healthDecisionService.decideDailyPlanGeneration(u, tomorrow, now);
                    if (d2.actions() != null && d2.actions().contains(DecisionActionType.GENERATE_DAILY_PLAN)) {
                        dailyPlanService.generateAndPersist(u, tomorrow);
                    }
                } catch (Exception ignored) {
                }
            }
            if (!users.hasNext()) break;
            page++;
        }
    }
}
