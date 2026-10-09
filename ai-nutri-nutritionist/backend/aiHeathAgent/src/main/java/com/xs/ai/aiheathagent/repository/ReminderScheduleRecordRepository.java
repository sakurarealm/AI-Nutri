package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.ReminderScheduleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderScheduleRecordRepository extends JpaRepository<ReminderScheduleRecord, Long> {
    Optional<ReminderScheduleRecord> findByUserIdAndScheduleKey(Long userId, String scheduleKey);

    List<ReminderScheduleRecord> findByEnabledTrue();

    List<ReminderScheduleRecord> findByUserIdAndEnabledTrue(Long userId);
}

