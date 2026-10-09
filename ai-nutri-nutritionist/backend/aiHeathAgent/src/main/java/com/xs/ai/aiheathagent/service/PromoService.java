package com.xs.ai.aiheathagent.service;


import com.xs.ai.aiheathagent.repo.WalletTransactionRepo;
import com.xs.ai.aiheathagent.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;


@Service @RequiredArgsConstructor
public class PromoService {
    private final WalletTransactionRepo txRepo; private final WalletService walletService;
    private static final String DEMO_CODE = "K-6666-8888-9999";


    @Transactional
    public int redeem(Long userId, String code){
        String uc = code==null? "": code.trim().toUpperCase();
        if (!DEMO_CODE.equals(uc)) throw new IllegalArgumentException("兑换码无效");
        String memo = "redeem:"+uc;
        if (txRepo.existsByUserIdAndTypeAndMemo(userId, "adjust", memo)) throw new IllegalStateException("该账号已兑换过");
        int bonus = 9999;
        walletService.addKCoins(userId, new java.math.BigDecimal(bonus), "adjust", null, memo);
        return bonus;
    }
}