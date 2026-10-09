package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.NutritionMealRecord;
import com.xs.ai.aiheathagent.entity.NutritionMealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NutritionMealRecordRepository extends JpaRepository<NutritionMealRecord, Long> {
    List<NutritionMealRecord> findByUserIdAndMemberIdAndRecordDateOrderByIdAsc(Long userId, Long memberId, LocalDate recordDate);

    List<NutritionMealRecord> findByUserIdAndMemberIdAndRecordDateAndMealTypeOrderByIdAsc(Long userId, Long memberId, LocalDate recordDate, NutritionMealType mealType);

    List<NutritionMealRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(Long userId, LocalDate start, LocalDate end);

    long countByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    @Query("select count(distinct r.userId) from NutritionMealRecord r where r.recordDate = :date")
    long countDistinctUserIdByRecordDate(@Param("date") LocalDate date);

    @Query("select distinct r.userId from NutritionMealRecord r where r.recordDate = :date")
    List<Long> findDistinctUserIdByRecordDate(@Param("date") LocalDate date);

    long deleteByUserIdAndMemberIdAndRecordDate(Long userId, Long memberId, LocalDate recordDate);

    long deleteByUserIdAndMemberId(Long userId, Long memberId);
}
