package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.NutritionFoodPickRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NutritionFoodPickRecordRepository extends JpaRepository<NutritionFoodPickRecord, Long> {
}

