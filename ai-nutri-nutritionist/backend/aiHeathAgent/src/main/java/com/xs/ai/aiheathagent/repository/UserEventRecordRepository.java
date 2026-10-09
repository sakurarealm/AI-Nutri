package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.UserEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserEventRecordRepository extends JpaRepository<UserEventRecord, Long> {
    List<UserEventRecord> findByUserIdAndOccurredAtBetweenOrderByOccurredAtAsc(Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndEventTypeAndOccurredAtBetween(Long userId, String eventType, LocalDateTime start, LocalDateTime end);

    long countByEventTypeAndOccurredAtBetween(String eventType, LocalDateTime start, LocalDateTime end);

    @Query("""
            select count(distinct e.userId) from UserEventRecord e
            where e.eventType = :eventType
              and e.occurredAt is not null
              and e.occurredAt >= :start
              and e.occurredAt < :end
            """)
    long countDistinctUserByEventTypeBetween(
            @Param("eventType") String eventType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select function('date', e.occurredAt) as d, count(e) as cnt
            from UserEventRecord e
            where e.eventType = :eventType
              and e.occurredAt is not null
              and e.occurredAt >= :start
              and e.occurredAt < :end
            group by function('date', e.occurredAt)
            order by d asc
            """)
    List<Object[]> dailyCountByEventTypeBetween(
            @Param("eventType") String eventType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select function('date', e.occurredAt) as d, count(distinct e.userId) as uv
            from UserEventRecord e
            where e.eventType = :eventType
              and e.occurredAt is not null
              and e.occurredAt >= :start
              and e.occurredAt < :end
            group by function('date', e.occurredAt)
            order by d asc
            """)
    List<Object[]> dailyDistinctUserByEventTypeBetween(
            @Param("eventType") String eventType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Optional<UserEventRecord> findTopByUserIdOrderByOccurredAtDescIdDesc(Long userId);

    Optional<UserEventRecord> findTopByUserIdAndModalityOrderByOccurredAtDescIdDesc(Long userId, String modality);

    long deleteByUserIdIn(List<Long> userIds);
}
