package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    Optional<User> findByUid(String uid);

    // 【新增】根据OpenID查询用户
    Optional<User> findByOpenid(String openid);

    @Query("""
            select u from User u
            where u.status is null or u.status <> 0
            """)
    Page<User> findActiveUsers(Pageable pageable);

    @Query("""
            select u from User u
            where (u.status is null or u.status <> 0)
              and u.lastLoginAt is not null
              and u.lastLoginAt >= :after
            """)
    Page<User> findActiveUsersLoggedInAfter(@Param("after") LocalDateTime after, Pageable pageable);

    @Query("""
            select count(u) from User u
            where u.createdAt is not null
              and u.createdAt >= :after
            """)
    long countCreatedAtAfter(@Param("after") LocalDateTime after);

    @Query("""
            select count(u) from User u
            where u.createdAt is not null
              and u.createdAt >= :start
              and u.createdAt < :end
            """)
    long countCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select function('date', u.createdAt) as d, count(u) as cnt
            from User u
            where u.createdAt is not null
              and u.createdAt >= :start
              and u.createdAt < :end
            group by function('date', u.createdAt)
            order by d asc
            """)
    List<Object[]> dailyNewUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select count(u) from User u
            where (u.status is null or u.status <> 0)
              and u.lastLoginAt is not null
              and u.lastLoginAt >= :after
            """)
    long countActiveUsersLoggedInAfter(@Param("after") LocalDateTime after);

    @Query("""
            select u from User u
            where (:onlyActive = false or u.status is null or u.status <> 0)
              and (:onlyManaged = false or u.createdByAdminId is not null)
              and (:bizLine = '' or coalesce(u.bizLine, '') = :bizLine)
              and (:createdByAdminId is null or u.createdByAdminId = :createdByAdminId)
              and (
                :username is null or :username = ''
                or lower(coalesce(u.nickname, '')) like lower(concat('%', :username, '%'))
              )
              and (
                :account is null or :account = ''
                or lower(coalesce(u.uid, '')) like lower(concat('%', :account, '%'))
                or lower(coalesce(u.phone, '')) like lower(concat('%', :account, '%'))
                or lower(coalesce(u.email, '')) like lower(concat('%', :account, '%'))
              )
            """)
    Page<User> pageAdmin(
            @Param("username") String username,
            @Param("account") String account,
            @Param("onlyActive") boolean onlyActive,
            @Param("onlyManaged") boolean onlyManaged,
            @Param("bizLine") String bizLine,
            @Param("createdByAdminId") Long createdByAdminId,
            Pageable pageable
    );

    @Query("""
            select (count(u) > 0) from User u
            where u.createdByAdminId = :adminId
              and (u.status is null or u.status <> 0)
            """)
    boolean existsActiveUsersCreatedBy(@Param("adminId") Long adminId);
}
