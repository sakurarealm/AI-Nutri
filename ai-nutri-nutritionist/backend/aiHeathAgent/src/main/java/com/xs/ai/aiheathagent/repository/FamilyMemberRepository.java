package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByUserIdOrderByIdAsc(Long userId);

    Optional<FamilyMember> findByUserIdAndMemberKey(Long userId, String memberKey);

    Optional<FamilyMember> findByIdAndUserId(Long id, Long userId);
}

