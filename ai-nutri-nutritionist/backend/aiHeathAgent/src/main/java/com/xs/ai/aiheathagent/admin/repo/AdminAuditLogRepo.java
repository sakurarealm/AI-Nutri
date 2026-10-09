package com.xs.ai.aiheathagent.admin.repo;

import com.xs.ai.aiheathagent.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepo extends JpaRepository<AdminAuditLog, Long> {
    @Query("""
            select l from AdminAuditLog l
            where (:operatorAdminId is null or l.operatorAdminId = :operatorAdminId)
              and (:bizLine = '' or coalesce(l.operatorBizLine, '') = :bizLine)
              and (
                :viewerRoleLevel <> 1
                or l.operatorRoleLevel is null
                or l.operatorRoleLevel <> 1
                or l.operatorAdminId = :viewerAdminId
              )
            order by l.id desc
            """)
    Page<AdminAuditLog> pageByScope(
            @Param("operatorAdminId") Long operatorAdminId,
            @Param("bizLine") String bizLine,
            @Param("viewerRoleLevel") Integer viewerRoleLevel,
            @Param("viewerAdminId") Long viewerAdminId,
            Pageable pageable
    );
}
