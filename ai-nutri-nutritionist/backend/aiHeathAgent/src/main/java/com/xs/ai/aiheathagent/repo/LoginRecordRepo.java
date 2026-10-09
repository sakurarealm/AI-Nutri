package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.LoginRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoginRecordRepo extends JpaRepository<LoginRecord, Long> {
    Optional<LoginRecord> findTopByRefreshTokenOrderByIdDesc(String refreshToken);
    void deleteByUser_IdIn(List<Long> userIds);

    @Query("""
            select count(lr) from LoginRecord lr
            where lr.success = true
              and lr.createdAt is not null
              and lr.createdAt >= :start
              and lr.createdAt < :end
            """)
    long countSuccessBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select count(distinct lr.user.id) from LoginRecord lr
            where lr.success = true
              and lr.createdAt is not null
              and lr.createdAt >= :start
              and lr.createdAt < :end
            """)
    long countDistinctUserSuccessBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(avg(lr.durationSec), 0) from LoginRecord lr
            where lr.durationSec is not null
              and lr.createdAt is not null
              and lr.createdAt >= :start
              and lr.createdAt < :end
            """)
    Double avgDurationSecBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select function('date', lr.createdAt) as d, count(distinct lr.user.id) as uv
            from LoginRecord lr
            where lr.success = true
              and lr.createdAt is not null
              and lr.createdAt >= :start
              and lr.createdAt < :end
            group by function('date', lr.createdAt)
            order by d asc
            """)
    List<Object[]> dailyDauBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select function('date', lr.createdAt) as d, count(lr) as cnt
            from LoginRecord lr
            where lr.success = true
              and lr.createdAt is not null
              and lr.createdAt >= :start
              and lr.createdAt < :end
            group by function('date', lr.createdAt)
            order by d asc
            """)
    List<Object[]> dailySessionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select function('date', lr.createdAt) as d, coalesce(avg(lr.durationSec), 0) as v
            from LoginRecord lr
            where lr.durationSec is not null
              and lr.createdAt is not null
              and lr.createdAt >= :start
              and lr.createdAt < :end
            group by function('date', lr.createdAt)
            order by d asc
            """)
    List<Object[]> dailyAvgDurationSecBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(lr.channel, '') as ch, count(lr) as cnt
            from LoginRecord lr
            where lr.success = true
              and lr.createdAt is not null
              and lr.createdAt >= :start
              and lr.createdAt < :end
            group by coalesce(lr.channel, '')
            order by cnt desc
            """)
    List<Object[]> countByChannelBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select count(distinct u.id) from LoginRecord lr
            join lr.user u
            where lr.success = true
              and lr.createdAt is not null
              and lr.createdAt >= :activeStart
              and lr.createdAt < :activeEnd
              and u.createdAt is not null
              and u.createdAt >= :cohortStart
              and u.createdAt < :cohortEnd
            """)
    long countDistinctCohortActiveUsersBetween(
            @Param("cohortStart") LocalDateTime cohortStart,
            @Param("cohortEnd") LocalDateTime cohortEnd,
            @Param("activeStart") LocalDateTime activeStart,
            @Param("activeEnd") LocalDateTime activeEnd
    );
}
