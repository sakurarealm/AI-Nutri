package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthRecordRepo extends JpaRepository<HealthRecord, Long> {
    Page<HealthRecord> findAllByUserIdOrderByRecordedAtDesc(Long userId, Pageable pageable);
    Page<HealthRecord> findAllByUserIdAndTypeOrderByRecordedAtDesc(Long userId, String type, Pageable pageable);


    Optional<HealthRecord> findTopByUserIdAndTypeOrderByRecordedAtDesc(Long userId, String type);
    List<HealthRecord> findAllByUserIdAndTypeAndRecordedAtBetweenOrderByRecordedAtAsc(Long userId, String type, LocalDateTime from, LocalDateTime to);
}