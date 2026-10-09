package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.RefundOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundOrderRepo extends JpaRepository<RefundOrder, Long> {
    Optional<RefundOrder> findByIdempotentKey(String idempotentKey);

    boolean existsByUserIdAndStatus(Long userId, String status);
    void deleteByUserIdIn(List<Long> userIds);

    @Query("""
            select (count(o) > 0) from RefundOrder o
            join User u on u.id = o.userId
            where u.createdByAdminId = :adminId
              and o.status = 'pending'
            """)
    boolean existsPendingByCreatedByAdminId(@Param("adminId") Long adminId);
    @Query("""
            select o from RefundOrder o
            where (:userId is null or o.userId = :userId)
              and (:status = '' or o.status = :status)
              and (:status = '' or o.status = :status)
              and (:refundNo = '' or o.refundNo = :refundNo)
              and (:after is null or (o.createdAt is not null and o.createdAt >= :after))
              and (:before is null or (o.createdAt is not null and o.createdAt <= :before))
            order by o.id desc
            """)
    Page<RefundOrder> pageAdmin(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("refundNo") String refundNo,
            @Param("after") LocalDateTime after,
            @Param("before") LocalDateTime before,
            Pageable pageable
    );
}
