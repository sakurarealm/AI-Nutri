package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repository.ReminderIntentRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProactiveSchedulerService {
    private final ReminderIntentRecordRepository reminderIntentRecordRepository;
    private final ReminderTriggerService reminderTriggerService;
    private final ReminderScheduleService reminderScheduleService;
    private final UserRepo userRepo;

    public ProactiveSchedulerService(
            ReminderIntentRecordRepository reminderIntentRecordRepository,
            ReminderTriggerService reminderTriggerService,
            ReminderScheduleService reminderScheduleService,
            UserRepo userRepo
    ) {
        this.reminderIntentRecordRepository = reminderIntentRecordRepository;
        this.reminderTriggerService = reminderTriggerService;
        this.reminderScheduleService = reminderScheduleService;
        this.userRepo = userRepo;
    }

    @Scheduled(fixedDelayString = "${reminder.executor.interval-ms:60000}")
    public void executePendingReminderIntents() {
        LocalDateTime now = LocalDateTime.now();
        try {
            reminderScheduleService.materializeDueSchedules(now, 5);
        } catch (Exception ignored) {
        }
        List<ReminderIntentRecord> list = reminderIntentRecordRepository.findByStatusAndValidUntilAfterOrderByCreatedAtAsc(
                "PENDING",
                now,
                PageRequest.of(0, 200)
        );
        Map<Long, User> userById = new HashMap<>();
        try {
            java.util.Set<Long> ids = new java.util.HashSet<>();
            for (ReminderIntentRecord intent : list) {
                if (intent == null || intent.getUserId() == null) continue;
                ids.add(intent.getUserId());
            }
            if (!ids.isEmpty()) {
                Iterable<User> users = userRepo.findAllById(ids);
                for (User u : users) {
                    if (u == null || u.getId() == null) continue;
                    userById.put(u.getId(), u);
                }
            }
        } catch (Exception ignored) {
        }
        for (ReminderIntentRecord intent : list) {
            try {
                User user = intent == null ? null : userById.get(intent.getUserId());
                reminderTriggerService.executeOne(intent, user, now);
            } catch (Exception ignored) {
            }
        }
    }
}
