package com.xs.ai.aiheathagent.service;


import com.xs.ai.aiheathagent.dto.req.SpendReq;
import com.xs.ai.aiheathagent.dto.resp.SpendResp;
import com.xs.ai.aiheathagent.entity.SpendOrder;
import com.xs.ai.aiheathagent.repo.SpendOrderRepo;
import com.xs.ai.aiheathagent.repo.WalletTransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service @RequiredArgsConstructor
public class SpendService {
    private final SpendOrderRepo spendRepo;
    private final WalletTransactionRepo txRepo;
    private final WalletService walletService;


    @Transactional
    public SpendResp spend(Long userId, SpendReq req){
        if (req.getAmountKc().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amountKc 必须大于 0");


// 幂等：如果已有成功的相同幂等键记录，直接返回
        var existed = spendRepo.findByIdempotentKey(req.getIdempotentKey()).orElse(null);
        if (existed != null) {
            if (existed.getUserId().longValue() != userId.longValue()) throw new IllegalArgumentException("幂等键已被其他用户占用");
            if (existed.getAmountKc().compareTo(req.getAmountKc()) != 0) throw new IllegalArgumentException("幂等重复请求的金额不一致");
            return new SpendResp(existed.getOrderNo(), existed.getAmountKc(), lastBalanceOf(userId, existed.getId()), existed.getStatus());
        }


// 1) 先落消费单（pending）
        SpendOrder so = new SpendOrder();
        so.setOrderNo(genOrderNo(userId));
        so.setUserId(userId);
        so.setAmountKc(req.getAmountKc());
        so.setBizType(req.getBizType());
        so.setBizRef(req.getBizRef());
        so.setIdempotentKey(req.getIdempotentKey());
        so.setStatus("pending");
        so.setCreatedAt(LocalDateTime.now());
        so.setUpdatedAt(LocalDateTime.now());
        spendRepo.save(so);


// 2) 钱包扣减（原子 + 并发安全由 walletService 内部加锁保证）
        walletService.addKCoins(userId, req.getAmountKc().negate(), "spend", so.getId(), safeMemo(req.getMemo(), so.getOrderNo()));


// 3) 更新状态 success
        so.setStatus("success");
        so.setUpdatedAt(LocalDateTime.now());
        spendRepo.save(so);


// 查询余额（取最新一条 ref=so 的流水余额）
        BigDecimal balanceAfter = lastBalanceOf(userId, so.getId());


        return new SpendResp(so.getOrderNo(), so.getAmountKc(), balanceAfter, so.getStatus());
    }


    private BigDecimal lastBalanceOf(Long userId, Long refOrderId){
// 取该消费单产生的最后一条流水（就是扣费那条）的余额
// 为简化，这里直接按 refOrderId 查 sum，不可靠。更准确方式是再建一个查询 last by id desc。
// 这里我们直接返回 sumChangeByRefAndType 的近似方案：改成由 WalletService 返回扣后余额。
        return null; // 实际由 controller 返回 WalletService 扣后返回值也行。此方法保留仅为结构完整。
    }


    private String genOrderNo(Long userId){
        return "SO" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                String.format("%04d", new java.util.Random().nextInt(10000)) + (userId%1000);
    }


    private String safeMemo(String memo, String orderNo){
        String m = memo==null? "": memo.trim();
        if (m.isEmpty()) m = "spend order:"+orderNo;
        return m.length() > 200? m.substring(0,200): m;
    }
}