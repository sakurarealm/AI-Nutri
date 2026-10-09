package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.SpendOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpendOrderRepository extends JpaRepository<SpendOrder, Long> {
}