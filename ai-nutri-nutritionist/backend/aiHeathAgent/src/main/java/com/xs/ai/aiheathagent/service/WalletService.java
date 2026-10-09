package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.dto.resp.PageResp;
import com.xs.ai.aiheathagent.dto.resp.TxItemResp;
import com.xs.ai.aiheathagent.dto.resp.WalletOverviewResp;
import com.xs.ai.aiheathagent.entity.WalletAccount;
import com.xs.ai.aiheathagent.entity.WalletTransaction;
import com.xs.ai.aiheathagent.repo.WalletAccountRepo;
import com.xs.ai.aiheathagent.repo.WalletTransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletAccountRepo walletRepo;
    private final WalletTransactionRepo txRepo;

    /** 确保账号存在（不存在则创建） */
    public WalletAccount ensureAccount(Long userId){
        return walletRepo.findByUserId(userId).orElseGet(() -> {
            WalletAccount w = new WalletAccount();
            w.setUserId(userId);
            w.setBalanceKc(BigDecimal.ZERO);
            w.setTotalRecharge(BigDecimal.ZERO);
            w.setTotalSpent(BigDecimal.ZERO);
            w.setUpdatedAt(LocalDateTime.now());
            return walletRepo.save(w);
        });
    }

    /** 钱包概览：余额、累计充值（K币口径）、累计消费（K币口径） */
    public WalletOverviewResp overview(Long userId){
        WalletAccount w = ensureAccount(userId);
        return new WalletOverviewResp(
                w.getBalanceKc(),
                w.getTotalRecharge(),
                w.getTotalSpent()
        );
    }

    /** 钱包流水分页 */
    public PageResp<TxItemResp> pageTx(Long userId, int page, int pageSize){
        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        Page<WalletTransaction> pg = txRepo.findAllByUserIdOrderByIdDesc(userId, pageable);
        List<TxItemResp> list = pg.getContent().stream()
                .map(x -> new TxItemResp(
                        x.getId(),
                        x.getType(),
                        x.getChangeKc(),
                        x.getBalanceAfter(),
                        x.getMemo(),
                        x.getCreatedAt()
                ))
                .toList();
        return new PageResp<>(pg.getTotalElements(), list);
    }

    /**
     * 增/减 K币（change 可正可负），返回扣后余额。
     * type: recharge / spend / refund / adjust
     */
    @Transactional
    public BigDecimal addKCoins(Long userId, BigDecimal change, String type, Long refOrderId, String memo){
        WalletAccount w = walletRepo.findByUserId(userId).orElseGet(() -> ensureAccount(userId));

        BigDecimal after = w.getBalanceKc().add(change);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("余额不足");
        }

        w.setBalanceKc(after);
        if ("recharge".equals(type)) {
            // 注意：这里按“K币口径”累计充值；若需要“人民币口径”，请改为按订单金额累计
            w.setTotalRecharge(w.getTotalRecharge().add(change));
        }
        if ("spend".equals(type)) {
            w.setTotalSpent(w.getTotalSpent().add(change.abs()));
        }
        w.setUpdatedAt(LocalDateTime.now());
        walletRepo.save(w);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setChangeKc(change);
        tx.setBalanceAfter(after);
        tx.setRefOrderId(refOrderId);
        tx.setMemo(memo);
        tx.setCreatedAt(LocalDateTime.now());
        txRepo.save(tx);

        return after;
    }
}
