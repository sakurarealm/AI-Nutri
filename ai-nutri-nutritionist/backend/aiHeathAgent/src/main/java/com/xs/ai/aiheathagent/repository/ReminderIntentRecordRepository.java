package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.ReminderIntentRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderIntentRecordRepository extends JpaRepository<ReminderIntentRecord, Long> {
    List<ReminderIntentRecord> findByStatusAndValidUntilAfterOrderByCreatedAtAsc(String status, LocalDateTime now);

    List<ReminderIntentRecord> findByStatusAndValidUntilAfterOrderByCreatedAtAsc(String status, LocalDateTime now, Pageable pageable);

    List<ReminderIntentRecord> findByUserIdAndPlanDateAndItemKeyOrderByCreatedAtDesc(Long userId, LocalDate planDate, String itemKey);

    boolean existsByUserIdAndDecisionKey(Long userId, String decisionKey);

    List<ReminderIntentRecord> findByUserIdAndStatusAndValidUntilAfterOrderByCreatedAtAsc(Long userId, String status, LocalDateTime now);

    long countByUserIdAndStatusAndValidUntilAfter(Long userId, String status, LocalDateTime now);

    Optional<ReminderIntentRecord> findTopByUserIdAndStatusAndValidUntilAfterOrderByCreatedAtAscIdAsc(Long userId, String status, LocalDateTime now);

    List<ReminderIntentRecord> findByUserIdAndPlanDate(Long userId, LocalDate planDate);

    Optional<ReminderIntentRecord> findTopByUserIdAndDecisionKeyOrderByCreatedAtDescIdDesc(Long userId, String decisionKey);
}
