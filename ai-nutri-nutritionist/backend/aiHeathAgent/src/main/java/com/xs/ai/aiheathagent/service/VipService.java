package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.dto.req.VipCreateOrderReq;
import com.xs.ai.aiheathagent.dto.resp.VipOrderResp;
import com.xs.ai.aiheathagent.dto.resp.VipPlanResp;
import com.xs.ai.aiheathagent.entity.UserVipSubscription;
import com.xs.ai.aiheathagent.entity.VipOrder;
import com.xs.ai.aiheathagent.repo.UserVipSubscriptionRepo;
import com.xs.ai.aiheathagent.repo.VipOrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VipService {
    private final UserVipSubscriptionRepo subscriptionRepo;
    private final VipOrderRepo vipOrderRepo;

    public List<VipPlanResp> plans() {
        return List.of(
                new VipPlanResp(
                        "basic",
                        "基础版",
                        "智能菜谱 + 健康监测",
                        new BigDecimal("39"),
                        "￥39/月",
                        30,
                        true,
                        "",
                        "#22c55e",
                        List.of("智能菜谱推荐", "健康数据趋势", "更细的饮食记录"),
                        List.of("智能菜谱推荐", "健康监测趋势追踪", "饮食记录与统计", "基础分析报告")
                ),
                new VipPlanResp(
                        "pro",
                        "专业版",
                        "含专业营养师季度视频问诊（每月4次，需预约）",
                        new BigDecimal("99"),
                        "￥99/月",
                        30,
                        true,
                        "推荐",
                        "#f59e0b",
                        List.of("每月 4 次问诊", "更详细数据分析", "专属计划更精准"),
                        List.of("智能菜谱推荐", "健康监测趋势追踪", "更详细的数据分析", "专业营养师视频问诊（每月4次，需预约）", "更精细的计划建议")
                )
        );
    }

    public Map<String, Object> vipView(Long userId) {
        Long uid = userId == null ? 0L : userId;
        Optional<UserVipSubscription> opt = subscriptionRepo.findByUserId(uid);
        UserVipSubscription sub = opt.orElse(null);

        int level = 0;
        String planId = "";
        boolean autoRenew = false;
        int familySeats = 0;
        LocalDateTime expireAt = null;
        if (sub != null) {
            level = sub.getLevel() == null ? 0 : sub.getLevel();
            planId = String.valueOf(sub.getPlanId() == null ? "" : sub.getPlanId());
            autoRenew = sub.getAutoRenew() != null && sub.getAutoRenew();
            familySeats = sub.getFamilySeats() == null ? 0 : sub.getFamilySeats();
            expireAt = sub.getExpireAt();
        }

        String levelName = resolveLevelName(level, planId);
        String expireDate = expireAt == null ? null : expireAt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<String> benefits = benefitsByPlan(planId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("level", level);
        payload.put("levelName", levelName);
        payload.put("expireDate", expireDate);
        payload.put("benefits", benefits);
        payload.put("planId", planId);
        payload.put("familySeats", Math.max(0, familySeats));
        payload.put("autoRenew", autoRenew);
        payload.put("active", isActive(expireAt));
        return payload;
    }

    @Transactional
    public VipOrderResp createOrder(Long userId, VipCreateOrderReq req) {
        Long uid = userId == null ? 0L : userId;
        String planId = req == null ? "" : String.valueOf(req.getPlanId() == null ? "" : req.getPlanId()).trim();
        int months = req == null || req.getMonths() == null ? 1 : req.getMonths();
        months = Math.max(1, Math.min(24, months));

        int familySeats = 0;
        if (req != null && req.getFamilySeats() != null) {
            familySeats = Math.max(0, Math.min(7, req.getFamilySeats()));
        }

        boolean autoRenew = req != null && req.getAutoRenew() != null && req.getAutoRenew();

        VipPlanResp plan = plans().stream().filter(p -> p.getId().equals(planId)).findFirst().orElse(null);
        if (plan == null) throw new IllegalArgumentException("未知套餐 planId");

        BigDecimal amount = plan.getPrice().multiply(new BigDecimal(months)).setScale(2, RoundingMode.HALF_UP);

        VipOrder o = new VipOrder();
        o.setOrderNo(generateOrderNo());
        o.setUserId(uid);
        o.setPlanId(planId);
        o.setMonths(months);
        o.setFamilySeats(0);
        o.setAutoRenew(autoRenew);
        o.setAmountCny(amount);
        o.setStatus("pending");
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        vipOrderRepo.save(o);

        return toResp(o);
    }

    public VipOrderResp getOrderOrThrow(Long userId, Long orderId) {
        VipOrder o = vipOrderRepo.findById(orderId).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (userId == null || o.getUserId() == null || !o.getUserId().equals(userId)) throw new IllegalArgumentException("无权访问订单");
        return toResp(o);
    }

    @Transactional
    public VipOrderResp payOrder(Long userId, Long orderId, String notifyPayload) {
        VipOrder o = vipOrderRepo.findById(orderId).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (userId == null || o.getUserId() == null || !o.getUserId().equals(userId)) throw new IllegalArgumentException("无权访问订单");
        if ("paid".equalsIgnoreCase(String.valueOf(o.getStatus()))) return toResp(o);

        o.setStatus("paid");
        o.setPaidAt(LocalDateTime.now());
        o.setNotifyPayload(notifyPayload == null ? "{\"mock\":true}" : notifyPayload);
        o.setUpdatedAt(LocalDateTime.now());
        vipOrderRepo.save(o);

        applyVipBenefit(o.getUserId(), o.getPlanId(), o.getMonths(), o.getAutoRenew());
        return toResp(o);
    }

    @Transactional
    public void notifyPaid(String orderNo, boolean success, String notifyPayload) {
        String no = String.valueOf(orderNo == null ? "" : orderNo).trim();
        if (no.isEmpty() || !success) return;
        VipOrder o = vipOrderRepo.findByOrderNo(no).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if ("paid".equalsIgnoreCase(String.valueOf(o.getStatus()))) return;
        o.setStatus("paid");
        o.setPaidAt(LocalDateTime.now());
        o.setNotifyPayload(notifyPayload == null ? "{\"notify\":true}" : notifyPayload);
        o.setUpdatedAt(LocalDateTime.now());
        vipOrderRepo.save(o);
        applyVipBenefit(o.getUserId(), o.getPlanId(), o.getMonths(), o.getAutoRenew());
    }

    @Transactional
    public UserVipSubscription applyVipBenefit(Long userId, String planId, Integer months, boolean autoRenew) {
        Long uid = userId == null ? 0L : userId;
        int m = months == null ? 1 : months;
        m = Math.max(1, Math.min(24, m));
        String pid = String.valueOf(planId == null ? "" : planId).trim();
        int level = "pro".equals(pid) ? 2 : "basic".equals(pid) ? 1 : 1;

        UserVipSubscription sub = subscriptionRepo.findByUserId(uid).orElse(null);
        if (sub == null) {
            sub = new UserVipSubscription();
            sub.setUserId(uid);
            sub.setLevel(level);
            sub.setPlanId(pid);
            sub.setAutoRenew(autoRenew);
            sub.setFamilySeats(0);
            sub.setCreatedAt(LocalDateTime.now());
        } else {
            sub.setLevel(Math.max(sub.getLevel() == null ? 0 : sub.getLevel(), level));
            sub.setPlanId(pid);
            sub.setAutoRenew(autoRenew);
            if (sub.getFamilySeats() == null) sub.setFamilySeats(0);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = sub.getExpireAt() != null && sub.getExpireAt().isAfter(now) ? sub.getExpireAt() : now;
        sub.setExpireAt(base.plusMonths(m));
        sub.setUpdatedAt(LocalDateTime.now());
        return subscriptionRepo.save(sub);
    }

    private VipOrderResp toResp(VipOrder o) {
        return new VipOrderResp(
                o.getId(),
                o.getOrderNo(),
                o.getPlanId(),
                o.getMonths(),
                o.getFamilySeats(),
                o.getAutoRenew(),
                o.getAmountCny(),
                o.getStatus(),
                o.getPaidAt(),
                o.getCreatedAt()
        );
    }

    private boolean isActive(LocalDateTime expireAt) {
        if (expireAt == null) return false;
        return expireAt.isAfter(LocalDateTime.now());
    }

    private List<String> benefitsByPlan(String planId) {
        String pid = String.valueOf(planId == null ? "" : planId).trim();
        if ("pro".equals(pid)) {
            return List.of("智能菜谱", "健康监测", "专业营养师视频问诊（每月4次，需预约）", "更详细的数据分析");
        }
        if ("basic".equals(pid)) {
            return List.of("智能菜谱", "健康监测");
        }
        return List.of();
    }

    private String resolveLevelName(int level, String planId) {
        String pid = String.valueOf(planId == null ? "" : planId).trim();
        if ("pro".equals(pid) || level >= 2) return "专业版";
        if ("basic".equals(pid) || level == 1) return "基础版";
        return "普通会员";
    }

    private String generateOrderNo() {
        return "V" + UUID.randomUUID().toString().replace("-", "");
    }
}

