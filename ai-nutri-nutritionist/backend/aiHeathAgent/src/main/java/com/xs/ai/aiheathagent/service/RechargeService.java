package com.xs.ai.aiheathagent.service;


import com.xs.ai.aiheathagent.dto.req.CreateOrderReq;
import com.xs.ai.aiheathagent.dto.resp.CreateOrderResp;
import com.xs.ai.aiheathagent.entity.*;
import com.xs.ai.aiheathagent.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


@Service @RequiredArgsConstructor
public class RechargeService {
    private final RechargePackageRepo pkgRepo;
    private final PaymentOrderRepo orderRepo;


    public List<RechargePackage> listActive(){
        return pkgRepo.findAllByStatusOrderBySortNoAscIdAsc(1);
    }


    @Transactional
    public CreateOrderResp createOrder(Long userId, CreateOrderReq req){
        RechargePackage pkg = pkgRepo.findById(req.getPackageId()).orElseThrow(() -> new IllegalArgumentException("套餐不存在"));
        if (pkg.getStatus()==null || pkg.getStatus()!=1) throw new IllegalArgumentException("套餐已下线");


        String orderNo = genOrderNo(userId);
        PaymentOrder o = new PaymentOrder();
        o.setOrderNo(orderNo);
        o.setUserId(userId);
        o.setPackageId(pkg.getId());
        o.setPayChannel(req.getPayChannel());
        o.setAmountCny(pkg.getPriceCny());
        o.setKcoins(pkg.getKcoins());
        o.setStatus("pending");
        o.setPrepayId("mock_"+orderNo);
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(o);


        return new CreateOrderResp(orderNo, req.getPayChannel(), pkg.getPriceCny(), pkg.getKcoins(), pkg.getGift(), o.getPrepayId(), o.getStatus());
    }


    public Optional<PaymentOrder> getByOrderNo(String orderNo){ return orderRepo.findByOrderNo(orderNo); }


    private String genOrderNo(Long userId){
        return "PO" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                String.format("%04d", new java.util.Random().nextInt(10000)) + (userId%1000);
    }
}