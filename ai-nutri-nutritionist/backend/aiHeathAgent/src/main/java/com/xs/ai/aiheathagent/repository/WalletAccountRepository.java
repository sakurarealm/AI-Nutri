package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.WalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {
    WalletAccount findByUserId(Long userId);
}