package com.xs.ai.aiheathagent.repo;


import com.xs.ai.aiheathagent.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;


public interface UserProfileRepo extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);
}