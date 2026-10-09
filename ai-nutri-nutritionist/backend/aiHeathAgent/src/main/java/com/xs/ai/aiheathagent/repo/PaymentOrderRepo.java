package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepo extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderNo(String orderNo);
    boolean existsByUserIdAndStatus(Long userId, String status);
    void deleteByUserIdIn(List<Long> userIds);

    @Query("""
            select (count(o) > 0) from PaymentOrder o
            join User u on u.id = o.userId
            where u.createdByAdminId = :adminId
              and o.status = 'pending'
            """)
    boolean existsPendingByCreatedByAdminId(@Param("adminId") Long adminId);

    @Query("""
            select o from PaymentOrder o
            where (:userId is null or o.userId = :userId)
              and (:status = '' or o.status = :status)
              and (:orderNo = '' or o.orderNo = :orderNo)
              and (:after is null or (o.createdAt is not null and o.createdAt >= :after))
              and (:before is null or (o.createdAt is not null and o.createdAt <= :before))
            order by o.id desc
            """)
    Page<PaymentOrder> pageAdmin(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("orderNo") String orderNo,
            @Param("after") LocalDateTime after,
            @Param("before") LocalDateTime before,
            Pageable pageable
    );

    @Query("""
            select coalesce(sum(o.amountCny), 0) from PaymentOrder o
            where o.status = 'paid'
              and o.paidAt is not null
              and o.paidAt >= :after
            """)
    BigDecimal sumPaidAmountCnyAfter(@Param("after") LocalDateTime after);
}
