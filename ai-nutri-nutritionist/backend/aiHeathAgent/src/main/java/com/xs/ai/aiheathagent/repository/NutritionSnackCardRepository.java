package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.NutritionSnackCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NutritionSnackCardRepository extends JpaRepository<NutritionSnackCard, Long> {
    List<NutritionSnackCard> findByUserIdAndMemberIdAndRecordDateOrderByIdAsc(Long userId, Long memberId, LocalDate recordDate);

    long deleteByUserIdAndMemberIdAndRecordDate(Long userId, Long memberId, LocalDate recordDate);

    long deleteByUserIdAndMemberId(Long userId, Long memberId);
}
