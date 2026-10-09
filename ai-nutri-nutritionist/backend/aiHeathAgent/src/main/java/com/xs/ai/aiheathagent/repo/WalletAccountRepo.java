package com.xs.ai.aiheathagent.repo;

import com.xs.ai.aiheathagent.entity.WalletAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletAccountRepo extends JpaRepository<WalletAccount, Long> {
    Optional<WalletAccount> findByUserId(Long userId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletAccount w where w.userId=:uid")
    Optional<WalletAccount> lockByUserId(@Param("uid") Long userId);
}