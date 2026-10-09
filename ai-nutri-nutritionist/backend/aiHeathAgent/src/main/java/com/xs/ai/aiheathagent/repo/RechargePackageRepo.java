package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.RechargePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RechargePackageRepo extends JpaRepository<RechargePackage, Long> {
    List<RechargePackage> findAllByStatusOrderBySortNoAscIdAsc(Integer status);
}
