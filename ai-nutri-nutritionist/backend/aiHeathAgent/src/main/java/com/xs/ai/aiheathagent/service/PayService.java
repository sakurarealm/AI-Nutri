package com.xs.ai.aiheathagent.service;


import com.xs.ai.aiheathagent.entity.*;
import com.xs.ai.aiheathagent.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service @RequiredArgsConstructor
public class PayService {
    private final PaymentOrderRepo orderRepo;
    private final RechargePackageRepo pkgRepo;
    private final WalletService walletService;


    /** idempotent: 多次回调只首次生效 */
    @Transactional
    public void markPaid(String orderNo, String notifyJson){
        PaymentOrder o = orderRepo.findByOrderNo(orderNo).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if ("paid".equals(o.getStatus())) return; // 幂等
        o.setStatus("paid");
        o.setPaidAt(LocalDateTime.now());
        o.setNotifyPayload(notifyJson);
        o.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(o);


// 入账 K币 = kcoins + gift
        RechargePackage pkg = o.getPackageId()==null? null : pkgRepo.findById(o.getPackageId()).orElse(null);
        int base = o.getKcoins()==null? 0: o.getKcoins();
        int gift = (pkg==null || pkg.getGift()==null)? 0: pkg.getGift();
        int total = base + gift; // 以 K币为单位


        walletService.addKCoins(o.getUserId(), new BigDecimal(total), "recharge", o.getId(), "recharge order:"+o.getOrderNo());
    }
}