package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepo extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndTypeAndMemo(Long userId, String type, String memo);
    void deleteByUserIdIn(List<Long> userIds);

    /** 统计某个 refOrderId 下、某类型流水的变更和（用于计算已退款额等） */
    @Query("select coalesce(sum(t.changeKc), 0) from WalletTransaction t " +
            "where t.userId = :uid and t.refOrderId = :oid and t.type = :type")
    BigDecimal sumChangeByRefAndType(@Param("uid") Long userId,
                                     @Param("oid") Long refOrderId,
                                     @Param("type") String type);

    /** 读取与 refOrderId 关联的最新一条流水（拿余额用） */
    Optional<WalletTransaction> findTopByUserIdAndRefOrderIdOrderByIdDesc(Long userId, Long refOrderId);

    @Query("""
            select t from WalletTransaction t
            where (:userId is null or t.userId = :userId)
              and (:type = '' or t.type = :type)
              and (:after is null or (t.createdAt is not null and t.createdAt >= :after))
              and (:before is null or (t.createdAt is not null and t.createdAt <= :before))
            order by t.id desc
            """)
    Page<WalletTransaction> pageAdmin(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("after") LocalDateTime after,
            @Param("before") LocalDateTime before,
            Pageable pageable
    );
}
