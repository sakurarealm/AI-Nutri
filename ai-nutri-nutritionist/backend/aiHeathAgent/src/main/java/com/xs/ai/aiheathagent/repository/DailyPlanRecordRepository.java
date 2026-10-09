package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.DailyPlanRecord;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPlanRecordRepository extends JpaRepository<DailyPlanRecord, Long> {
    Optional<DailyPlanRecord> findTopByUserIdAndPlanDateOrderByIdDesc(Long userId, LocalDate planDate);

    boolean existsByUserIdAndPlanDate(Long userId, LocalDate planDate);

    Optional<DailyPlanRecord> findTopByUserIdOrderByPlanDateDescIdDesc(Long userId);

    @Query("""
            select r.userId from DailyPlanRecord r
            where r.planDate = :planDate
            """)
    List<Long> listUserIdsByPlanDate(@Param("planDate") LocalDate planDate);
}
