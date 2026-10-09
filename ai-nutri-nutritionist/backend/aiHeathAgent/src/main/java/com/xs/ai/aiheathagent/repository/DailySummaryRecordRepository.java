package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.DailySummaryRecord;
import com.xs.ai.aiheathagent.conversation.explain.ExplanationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySummaryRecordRepository extends JpaRepository<DailySummaryRecord, Long> {
    Optional<DailySummaryRecord> findTopByUserIdAndDateOrderByIdDesc(Long userId, LocalDate date);

    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    boolean existsByUserIdAndDateAndExplanationCode(Long userId, LocalDate date, ExplanationCode explanationCode);

    @Query("select distinct r.userId from DailySummaryRecord r where r.date = :date and r.explanationCode = :code")
    List<Long> listUserIdsByDateAndExplanationCode(@Param("date") LocalDate date, @Param("code") ExplanationCode code);

    Optional<DailySummaryRecord> findTopByUserIdOrderByDateDescIdDesc(Long userId);
}
