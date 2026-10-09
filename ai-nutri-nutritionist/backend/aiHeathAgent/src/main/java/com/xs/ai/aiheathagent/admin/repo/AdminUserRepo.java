package com.xs.ai.aiheathagent.admin.repo;

import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminUserRepo extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUid(String uid);
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByEmail(String email);
    Optional<AdminUser> findByPhoneNumber(String phoneNumber);

    @Query("""
            select a from AdminUser a
            where (:onlyActive = false or a.status is null or a.status <> 0)
              and (
                :minRoleLevel is null
                or (:includeAdminId is not null and a.id = :includeAdminId)
                or (a.roleLevel is not null and a.roleLevel >= :minRoleLevel)
              )
              and (
                :maxRoleLevel is null
                or (:includeAdminId is not null and a.id = :includeAdminId)
                or (a.roleLevel is not null and a.roleLevel <= :maxRoleLevel)
              )
              and (:bizLine = '' or coalesce(a.bizLine, '') = :bizLine)
              and (:createdByAdminId is null or a.createdByAdminId = :createdByAdminId)
              and (
                :account = ''
                or lower(coalesce(a.username, '')) like lower(concat('%', :account, '%'))
                or lower(coalesce(a.uid, '')) like lower(concat('%', :account, '%'))
                or lower(coalesce(a.email, '')) like lower(concat('%', :account, '%'))
              )
            order by a.id desc
            """)
    Page<AdminUser> pageAdmin(
            @Param("account") String account,
            @Param("onlyActive") boolean onlyActive,
            @Param("minRoleLevel") Integer minRoleLevel,
            @Param("maxRoleLevel") Integer maxRoleLevel,
            @Param("bizLine") String bizLine,
            @Param("createdByAdminId") Long createdByAdminId,
            @Param("includeAdminId") Long includeAdminId,
            Pageable pageable
    );
}
