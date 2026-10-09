package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.dto.req.RefundReq;
import com.xs.ai.aiheathagent.dto.resp.RefundResp;
import com.xs.ai.aiheathagent.entity.RefundOrder;
import com.xs.ai.aiheathagent.entity.SpendOrder;
import com.xs.ai.aiheathagent.repo.RefundOrderRepo;
import com.xs.ai.aiheathagent.repo.SpendOrderRepo;
import com.xs.ai.aiheathagent.repo.WalletTransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final SpendOrderRepo spendRepo;
    private final RefundOrderRepo refundRepo;
    private final WalletTransactionRepo txRepo;
    private final WalletService walletService;

    @Transactional
    public RefundResp refund(Long userId, RefundReq req){
        // 幂等（退款级别）
        var existed = refundRepo.findByIdempotentKey(req.getIdempotentKey()).orElse(null);
        if (existed != null) {
            if (!existed.getUserId().equals(userId)) {
                throw new IllegalArgumentException("退款幂等键已被其他用户占用");
            }
            SpendOrder so = spendRepo.findById(existed.getSpendOrderId()).orElseThrow();
            BigDecimal balance = latestBalanceByRef(userId, so.getId()); // 读取该消费单相关的最新余额
            return new RefundResp(existed.getRefundNo(), so.getOrderNo(), existed.getAmountKc(), balance, existed.getStatus());
        }

        // 定位消费单（orderNo 优先）
        SpendOrder so;
        if (req.getOrderNo()!=null && !req.getOrderNo().isBlank()) {
            so = spendRepo.findByOrderNo(req.getOrderNo())
                    .orElseThrow(() -> new IllegalArgumentException("消费单不存在"));
        } else if (req.getSpendIdempotentKey()!=null && !req.getSpendIdempotentKey().isBlank()) {
            so = spendRepo.findByIdempotentKey(req.getSpendIdempotentKey())
                    .orElseThrow(() -> new IllegalArgumentException("消费单不存在"));
        } else {
            throw new IllegalArgumentException("必须提供 orderNo 或 spendIdempotentKey");
        }
        if (!so.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权退款此订单");
        }

        // 可退款 = 消费金额 - 历史已退（退款流水是正数）
        BigDecimal spent = so.getAmountKc();
        BigDecimal refunded = txRepo.sumChangeByRefAndType(userId, so.getId(), "refund");
        if (refunded == null) refunded = BigDecimal.ZERO;
        BigDecimal refundable = spent.subtract(refunded);
        if (refundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("该订单已无可退金额");
        }

        BigDecimal toRefund = (req.getRefundAmountKc()!=null ? req.getRefundAmountKc() : refundable);
        if (toRefund.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额必须大于 0");
        }
        if (toRefund.compareTo(refundable) > 0) {
            toRefund = refundable; // 上限保护
        }

        // 1) 生成退款单（pending）
        RefundOrder ro = new RefundOrder();
        ro.setRefundNo(genRefundNo(userId));
        ro.setUserId(userId);
        ro.setSpendOrderId(so.getId());
        ro.setAmountKc(toRefund);
        ro.setIdempotentKey(req.getIdempotentKey());
        ro.setStatus("pending");
        ro.setCreatedAt(LocalDateTime.now());
        ro.setUpdatedAt(LocalDateTime.now());
        refundRepo.save(ro);

        // 2) 钱包入账（refund）—— 关键：addKCoins 返回扣后余额
        BigDecimal balanceAfter = walletService.addKCoins(
                userId, toRefund, "refund", so.getId(), safeMemo(req.getMemo(), ro.getRefundNo())
        );

        // 3) 更新退款单 & 消费单状态
        ro.setStatus("success");
        ro.setUpdatedAt(LocalDateTime.now());
        refundRepo.save(ro);

        BigDecimal newRefunded = refunded.add(toRefund);
        if (newRefunded.compareTo(spent) >= 0) {
            so.setStatus("refunded");
        } else {
            so.setStatus("partial_refunded");
        }
        so.setUpdatedAt(LocalDateTime.now());
        spendRepo.save(so);

        return new RefundResp(ro.getRefundNo(), so.getOrderNo(), toRefund, balanceAfter, ro.getStatus());
    }

    private String genRefundNo(Long userId){
        return "RF" + LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new java.util.Random().nextInt(10000))
                + (userId % 1000);
    }

    /** 读取与某个 refOrderId 关联的最新一条流水余额（无则返回 null） */
    private BigDecimal latestBalanceByRef(Long userId, Long refOrderId){
        return txRepo.findTopByUserIdAndRefOrderIdOrderByIdDesc(userId, refOrderId)
                .map(t -> t.getBalanceAfter())
                .orElse(null);
    }

    private String safeMemo(String memo, String refundNo){
        String m = memo==null ? "" : memo.trim();
        if (m.isEmpty()) m = "refund order:" + refundNo;
        return m.length() > 200 ? m.substring(0,200) : m;
    }
}
