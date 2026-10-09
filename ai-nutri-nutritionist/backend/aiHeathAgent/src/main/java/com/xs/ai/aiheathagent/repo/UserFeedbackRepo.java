package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.UserFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFeedbackRepo extends JpaRepository<UserFeedback, Long> {
    @Query("""
            select f from UserFeedback f
            where (:q = '' or lower(coalesce(f.subject, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(f.content, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(f.orderNo, '')) like lower(concat('%', :q, '%')))
              and (:status is null or f.status = :status)
              and (:userId is null or f.userId = :userId)
            order by f.id desc
            """)
    Page<UserFeedback> pageAdmin(
            @Param("q") String q,
            @Param("status") Integer status,
            @Param("userId") Long userId,
            Pageable pageable
    );
}

