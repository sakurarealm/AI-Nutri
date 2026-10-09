package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.ConversationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRecordRepository extends JpaRepository<ConversationRecord, Long> {
    List<ConversationRecord> findBySessionIdOrderByTimestampAscIdAsc(String sessionId);

    List<ConversationRecord> findBySessionIdAndUserIdOrderByTimestampAscIdAsc(String sessionId, Long userId);

    List<ConversationRecord> findBySessionIdAndUserIdAndIdGreaterThanOrderByIdAsc(String sessionId, Long userId, Long id);

    Page<ConversationRecord> findBySessionIdAndUserIdOrderByIdDesc(String sessionId, Long userId, Pageable pageable);

    Page<ConversationRecord> findBySessionIdAndUserIdAndIdLessThanOrderByIdDesc(String sessionId, Long userId, Long beforeId, Pageable pageable);

    Optional<ConversationRecord> findTopBySessionIdAndUserIdOrderByTimestampDescIdDesc(String sessionId, Long userId);

    Optional<ConversationRecord> findTopByUserIdAndMessageTypeAndSessionIdNotOrderByTimestampDescIdDesc(
            Long userId, ConversationRecord.MessageType messageType, String sessionId
    );

    Optional<ConversationRecord> findTopByUserIdAndMessageTypeOrderByTimestampDescIdDesc(
            Long userId, ConversationRecord.MessageType messageType
    );

    long countByUserIdAndTimestampBetween(Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndMessageTypeAndTimestampBetween(Long userId, ConversationRecord.MessageType messageType, LocalDateTime start, LocalDateTime end);

    @Query("""
            select count(r) from ConversationRecord r
            where r.timestamp is not null
              and r.timestamp >= :start
              and r.timestamp < :end
            """)
    long countAllBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select count(r) from ConversationRecord r
            where r.messageType = :messageType
              and r.timestamp is not null
              and r.timestamp >= :start
              and r.timestamp < :end
            """)
    long countAllByMessageTypeBetween(
            @Param("messageType") ConversationRecord.MessageType messageType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select count(distinct r.userId) from ConversationRecord r
            where r.messageType = :messageType
              and r.timestamp is not null
              and r.timestamp >= :start
              and r.timestamp < :end
            """)
    long countDistinctUserByMessageTypeBetween(
            @Param("messageType") ConversationRecord.MessageType messageType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select function('date', r.timestamp) as d, count(r) as cnt
            from ConversationRecord r
            where r.messageType = :messageType
              and r.timestamp is not null
              and r.timestamp >= :start
              and r.timestamp < :end
            group by function('date', r.timestamp)
            order by d asc
            """)
    List<Object[]> dailyCountByMessageTypeBetween(
            @Param("messageType") ConversationRecord.MessageType messageType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select function('date', r.timestamp) as d, count(distinct r.userId) as uv
            from ConversationRecord r
            where r.messageType = :messageType
              and r.timestamp is not null
              and r.timestamp >= :start
              and r.timestamp < :end
            group by function('date', r.timestamp)
            order by d asc
            """)
    List<Object[]> dailyDistinctUserByMessageTypeBetween(
            @Param("messageType") ConversationRecord.MessageType messageType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Page<ConversationRecord> findByUserIdAndTimestampBetweenOrderByTimestampDescIdDesc(Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<ConversationRecord> findByUserIdAndMessageTypeAndTimestampBetweenOrderByTimestampDescIdDesc(Long userId, ConversationRecord.MessageType messageType, LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countBySessionIdAndUserIdAndTimestampAfter(String sessionId, Long userId, LocalDateTime after);

    @Query("""
            select count(r) from ConversationRecord r
            where r.sessionId = :sessionId
              and r.userId = :userId
              and r.timestamp >= :after
              and r.content not like concat(:excludePrefix, '%')
            """)
    long countBySessionIdAndUserIdAndTimestampAfterExcludePrefix(
            @Param("sessionId") String sessionId,
            @Param("userId") Long userId,
            @Param("after") LocalDateTime after,
            @Param("excludePrefix") String excludePrefix
    );

    @Query("""
            select r from ConversationRecord r
            where r.sessionId = :sessionId
              and r.userId = :userId
              and r.content not like concat(:excludePrefix, '%')
            order by r.timestamp desc, r.id desc
            """)
    List<ConversationRecord> findLatestBySessionIdAndUserIdExcludePrefix(
            @Param("sessionId") String sessionId,
            @Param("userId") Long userId,
            @Param("excludePrefix") String excludePrefix,
            Pageable pageable
    );

    boolean existsBySessionIdAndUserIdAndContentStartingWith(String sessionId, Long userId, String prefix);

    long deleteByUserIdIn(List<Long> userIds);
}
