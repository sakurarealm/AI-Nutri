package com.xs.ai.aiheathagent.admin.controller;

import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.admin.entity.AdminAuditLog;
import com.xs.ai.aiheathagent.admin.repo.AdminAuditLogRepo;
import com.xs.ai.aiheathagent.admin.repo.AdminRefreshTokenRepo;
import com.xs.ai.aiheathagent.admin.repo.AdminUserRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xs.ai.aiheathagent.admin.service.AdminUserService;
import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.resp.PageResp;
import com.xs.ai.aiheathagent.entity.ConversationRecord;
import com.xs.ai.aiheathagent.entity.FoodCatalog;
import com.xs.ai.aiheathagent.entity.PaymentOrder;
import com.xs.ai.aiheathagent.entity.Recipe;
import com.xs.ai.aiheathagent.entity.RecipeIngredient;
import com.xs.ai.aiheathagent.entity.RecipeStep;
import com.xs.ai.aiheathagent.entity.RefundOrder;
import com.xs.ai.aiheathagent.entity.SpendOrder;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.entity.VipOrder;
import com.xs.ai.aiheathagent.entity.WalletTransaction;
import com.xs.ai.aiheathagent.repo.LoginRecordRepo;
import com.xs.ai.aiheathagent.repo.PaymentOrderRepo;
import com.xs.ai.aiheathagent.repo.RefreshTokenRepo;
import com.xs.ai.aiheathagent.repo.RefundOrderRepo;
import com.xs.ai.aiheathagent.repo.SpendOrderRepo;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repo.VipOrderRepo;
import com.xs.ai.aiheathagent.repo.WalletTransactionRepo;
import com.xs.ai.aiheathagent.repository.ConversationRecordRepository;
import com.xs.ai.aiheathagent.repository.FoodCatalogRepository;
import com.xs.ai.aiheathagent.repository.RecipeRepository;
import com.xs.ai.aiheathagent.repository.UserEventRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/admin", "/api/api/admin", "/admin"})
public class AdminController {
    private final UserRepo userRepo;
    private final PaymentOrderRepo paymentOrderRepo;
    private final VipOrderRepo vipOrderRepo;
    private final SpendOrderRepo spendOrderRepo;
    private final RefundOrderRepo refundOrderRepo;
    private final WalletTransactionRepo walletTransactionRepo;
    private final LoginRecordRepo loginRecordRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final ConversationRecordRepository conversationRecordRepository;
    private final UserEventRecordRepository userEventRecordRepository;
    private final RecipeRepository recipeRepository;
    private final FoodCatalogRepository foodCatalogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepo adminAuditLogRepo;
    private final AdminUserRepo adminUserRepo;
    private final AdminRefreshTokenRepo adminRefreshTokenRepo;
    private final AdminUserService adminUserService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

    public AdminController(
            UserRepo userRepo,
            PaymentOrderRepo paymentOrderRepo,
            VipOrderRepo vipOrderRepo,
            SpendOrderRepo spendOrderRepo,
            RefundOrderRepo refundOrderRepo,
            WalletTransactionRepo walletTransactionRepo,
            LoginRecordRepo loginRecordRepo,
            RefreshTokenRepo refreshTokenRepo,
            ConversationRecordRepository conversationRecordRepository,
            UserEventRecordRepository userEventRecordRepository,
            RecipeRepository recipeRepository,
            FoodCatalogRepository foodCatalogRepository,
            PasswordEncoder passwordEncoder,
            AdminAuditLogRepo adminAuditLogRepo,
            AdminUserRepo adminUserRepo,
            AdminRefreshTokenRepo adminRefreshTokenRepo,
            AdminUserService adminUserService
    ) {
        this.userRepo = userRepo;
        this.paymentOrderRepo = paymentOrderRepo;
        this.vipOrderRepo = vipOrderRepo;
        this.spendOrderRepo = spendOrderRepo;
        this.refundOrderRepo = refundOrderRepo;
        this.walletTransactionRepo = walletTransactionRepo;
        this.loginRecordRepo = loginRecordRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.conversationRecordRepository = conversationRecordRepository;
        this.userEventRecordRepository = userEventRecordRepository;
        this.recipeRepository = recipeRepository;
        this.foodCatalogRepository = foodCatalogRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditLogRepo = adminAuditLogRepo;
        this.adminUserRepo = adminUserRepo;
        this.adminRefreshTokenRepo = adminRefreshTokenRepo;
        this.adminUserService = adminUserService;
    }

    private AdminUser currentAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        Object principal = auth.getPrincipal();
        if (principal instanceof AdminUser u) {
            if (u.getId() == null || u.getUid() == null || u.getUid().isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
            }
            AdminUser existed = adminUserRepo.findByUid(u.getUid()).orElse(null);
            if (existed != null) return existed;
            if (u.getId() != null) {
                AdminUser byId = adminUserRepo.findById(u.getId()).orElse(null);
                if (byId != null) return byId;
            }
            return u;
        }
        if (principal instanceof User u) {
            String role = safeStr(u.getRole()).trim().toUpperCase();
            if ("ADMIN".equals(role) && u.getId() != null && u.getUid() != null && !u.getUid().isBlank()) {
                AdminUser existed = adminUserRepo.findByUid(u.getUid()).orElse(null);
                if (existed != null) return existed;
                AdminUser byId = adminUserRepo.findById(u.getId()).orElse(null);
                if (byId != null) return byId;
                AdminUser a = new AdminUser();
                a.setId(u.getId());
                a.setUid(u.getUid());
                a.setUsername(u.getUid());
                a.setNickname(u.getNickname());
                a.setAvatarUrl(u.getAvatarUrl());
                a.setRole("ADMIN");
                a.setStatus(u.getStatus());
                a.setBizLine(u.getBizLine());
                return a;
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }

    @GetMapping("/ping")
    public ApiResp<Map<String, Object>> ping() {
        AdminUser u = currentAdmin();
        return ApiResp.ok(Map.of("ok", true, "uid", u.getUid()));
    }

    @GetMapping("/analysis/total")
    public ApiResp<Map<String, Object>> analysisTotal(
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeDays = Math.max(1, Math.min(days, 365));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(safeDays);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        long newUsers = 0;
        long dau = 0;
        long sessions = 0;
        double avgSessionSec = 0.0;
        long chatUserMsgs = 0;
        long nutritionDaySaves = 0;
        BigDecimal vipRevenue = BigDecimal.ZERO;
        long vipPaidOrders = 0;
        long vipPaidUsers = 0;

        try { newUsers = userRepo.countCreatedAtAfter(start); } catch (Exception ignored) {}
        try { dau = loginRecordRepo.countDistinctUserSuccessBetween(todayStart, todayEnd); } catch (Exception ignored) {}
        try { sessions = loginRecordRepo.countSuccessBetween(start, now); } catch (Exception ignored) {}
        try { avgSessionSec = loginRecordRepo.avgDurationSecBetween(start, now); } catch (Exception ignored) {}
        try { chatUserMsgs = conversationRecordRepository.countAllByMessageTypeBetween(ConversationRecord.MessageType.USER, start, now); } catch (Exception ignored) {}
        try { nutritionDaySaves = userEventRecordRepository.countByEventTypeAndOccurredAtBetween("NUTRITION_DAY_SAVE", start, now); } catch (Exception ignored) {}
        try { vipRevenue = vipOrderRepo.sumPaidAmountCnyBetween(start, now); } catch (Exception ignored) {}
        try { vipPaidOrders = vipOrderRepo.countPaidBetween(start, now); } catch (Exception ignored) {}
        try { vipPaidUsers = vipOrderRepo.countDistinctPaidUsersBetween(start, now); } catch (Exception ignored) {}

        Map<String, Object> out = new HashMap<>();
        out.put("rangeDays", safeDays);
        out.put("newUsers", newUsers);
        out.put("dau", dau);
        out.put("sessions", sessions);
        out.put("avgSessionSec", avgSessionSec);
        out.put("chatUserMsgs", chatUserMsgs);
        out.put("nutritionDaySaves", nutritionDaySaves);
        out.put("vipRevenueCny", vipRevenue == null ? BigDecimal.ZERO : vipRevenue);
        out.put("vipPaidOrders", vipPaidOrders);
        out.put("vipPaidUsers", vipPaidUsers);
        out.put("users", newUsers);
        out.put("messages", chatUserMsgs);
        out.put("moneys", (vipRevenue == null ? BigDecimal.ZERO : vipRevenue).doubleValue());
        out.put("shoppings", vipPaidOrders);

        writeAudit(admin, req, "ANALYSIS_TOTAL", "ANALYSIS", "", "success", Map.of("days", safeDays));
        return ApiResp.ok(out);
    }

    @GetMapping("/analysis/userAccessSource")
    public ApiResp<List<Map<String, Object>>> analysisUserAccessSource(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeDays = Math.max(1, Math.min(days, 365));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(safeDays);

        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<Object[]> rows = loginRecordRepo.countByChannelBetween(start, now);
            for (Object[] r : rows) {
                String name = r == null ? "" : String.valueOf(r[0] == null ? "" : r[0]).trim();
                if (name.isEmpty()) name = "unknown";
                long v = 0;
                if (r != null && r.length > 1 && r[1] instanceof Number n) v = n.longValue();
                out.add(Map.of("name", name, "value", v));
            }
        } catch (Exception ignored) {
        }

        writeAudit(admin, req, "ANALYSIS_ACCESS_SOURCE", "ANALYSIS", "", "success", Map.of("days", safeDays));
        return ApiResp.ok(out);
    }

    @GetMapping("/analysis/weeklyUserActivity")
    public ApiResp<List<Map<String, Object>>> analysisWeeklyUserActivity(
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeDays = Math.max(1, Math.min(days, 60));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> byDay = new HashMap<>();
        try {
            List<Object[]> rows = loginRecordRepo.dailyDauBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                byDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate d = startDate.plusDays(i);
            out.add(Map.of(
                    "name", d.toString(),
                    "value", byDay.getOrDefault(d, 0L)
            ));
        }

        writeAudit(admin, req, "ANALYSIS_DAU_DAILY", "ANALYSIS", "", "success", Map.of("days", safeDays));
        return ApiResp.ok(out);
    }

    @GetMapping("/analysis/monthlySales")
    public ApiResp<List<Map<String, Object>>> analysisMonthlySales(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeDays = Math.max(1, Math.min(days, 365));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> orderCnt = new HashMap<>();
        Map<LocalDate, BigDecimal> revenue = new HashMap<>();
        try {
            List<Object[]> rows = vipOrderRepo.dailyPaidStatsBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 3) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long cnt = (r[1] instanceof Number n) ? n.longValue() : 0L;
                BigDecimal amt = (r[2] instanceof BigDecimal bd) ? bd : BigDecimal.ZERO;
                orderCnt.put(d, Math.max(0, cnt));
                revenue.put(d, amt == null ? BigDecimal.ZERO : amt);
            }
        } catch (Exception ignored) {
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate d = startDate.plusDays(i);
            BigDecimal amt = revenue.getOrDefault(d, BigDecimal.ZERO);
            out.add(Map.of(
                    "name", d.toString(),
                    "estimate", orderCnt.getOrDefault(d, 0L),
                    "actual", amt
            ));
        }

        writeAudit(admin, req, "ANALYSIS_REVENUE_DAILY", "ANALYSIS", "", "success", Map.of("days", safeDays));
        return ApiResp.ok(out);
    }

    @GetMapping("/analysis/trends")
    public ApiResp<Map<String, Object>> analysisTrends(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeDays = Math.max(1, Math.min(days, 365));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> newUsersByDay = new HashMap<>();
        Map<LocalDate, Long> dauByDay = new HashMap<>();
        Map<LocalDate, Long> sessionsByDay = new HashMap<>();
        Map<LocalDate, Double> avgSessionSecByDay = new HashMap<>();
        Map<LocalDate, Long> chatUserMsgsByDay = new HashMap<>();
        Map<LocalDate, Long> nutritionSavesByDay = new HashMap<>();
        Map<LocalDate, Long> vipPaidOrdersByDay = new HashMap<>();
        Map<LocalDate, BigDecimal> vipRevenueByDay = new HashMap<>();
        Map<LocalDate, Long> vipPaidUsersByDay = new HashMap<>();

        try {
            List<Object[]> rows = userRepo.dailyNewUsersBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                newUsersByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = loginRecordRepo.dailyDauBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                dauByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = loginRecordRepo.dailySessionsBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                sessionsByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = loginRecordRepo.dailyAvgDurationSecBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                double v = (r[1] instanceof Number n) ? n.doubleValue() : 0.0;
                avgSessionSecByDay.put(d, Math.max(0.0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = conversationRecordRepository.dailyCountByMessageTypeBetween(ConversationRecord.MessageType.USER, start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                chatUserMsgsByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = userEventRecordRepository.dailyCountByEventTypeBetween("NUTRITION_DAY_SAVE", start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                nutritionSavesByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = vipOrderRepo.dailyPaidStatsBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 3) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long cnt = (r[1] instanceof Number n) ? n.longValue() : 0L;
                BigDecimal amt = (r[2] instanceof BigDecimal bd) ? bd : BigDecimal.ZERO;
                vipPaidOrdersByDay.put(d, Math.max(0, cnt));
                vipRevenueByDay.put(d, amt == null ? BigDecimal.ZERO : amt);
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = vipOrderRepo.dailyDistinctPaidUsersBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                vipPaidUsersByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }

        List<String> dates = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        List<Long> dau = new ArrayList<>();
        List<Long> sessions = new ArrayList<>();
        List<Double> avgSessionSec = new ArrayList<>();
        List<Long> chatUserMsgs = new ArrayList<>();
        List<Long> nutritionDaySaves = new ArrayList<>();
        List<Long> vipPaidOrders = new ArrayList<>();
        List<BigDecimal> vipRevenueCny = new ArrayList<>();
        List<Long> vipPaidUsers = new ArrayList<>();

        for (int i = 0; i < safeDays; i++) {
            LocalDate d = startDate.plusDays(i);
            dates.add(d.toString());
            newUsers.add(newUsersByDay.getOrDefault(d, 0L));
            dau.add(dauByDay.getOrDefault(d, 0L));
            sessions.add(sessionsByDay.getOrDefault(d, 0L));
            avgSessionSec.add(avgSessionSecByDay.getOrDefault(d, 0.0));
            chatUserMsgs.add(chatUserMsgsByDay.getOrDefault(d, 0L));
            nutritionDaySaves.add(nutritionSavesByDay.getOrDefault(d, 0L));
            vipPaidOrders.add(vipPaidOrdersByDay.getOrDefault(d, 0L));
            vipRevenueCny.add(vipRevenueByDay.getOrDefault(d, BigDecimal.ZERO));
            vipPaidUsers.add(vipPaidUsersByDay.getOrDefault(d, 0L));
        }

        Map<String, Object> out = new HashMap<>();
        out.put("rangeDays", safeDays);
        out.put("dates", dates);
        out.put("newUsers", newUsers);
        out.put("dau", dau);
        out.put("sessions", sessions);
        out.put("avgSessionSec", avgSessionSec);
        out.put("chatUserMsgs", chatUserMsgs);
        out.put("nutritionDaySaves", nutritionDaySaves);
        out.put("vipPaidOrders", vipPaidOrders);
        out.put("vipRevenueCny", vipRevenueCny);
        out.put("vipPaidUsers", vipPaidUsers);

        writeAudit(admin, req, "ANALYSIS_TRENDS", "ANALYSIS", "", "success", Map.of("days", safeDays));
        return ApiResp.ok(out);
    }

    @GetMapping("/analysis/funnel")
    public ApiResp<List<Map<String, Object>>> analysisFunnel(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeDays = Math.max(1, Math.min(days, 365));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long loginUsers = 0;
        long chatUsers = 0;
        long nutritionUsers = 0;
        long vipPaidUsers = 0;

        try { loginUsers = loginRecordRepo.countDistinctUserSuccessBetween(start, end); } catch (Exception ignored) {}
        try { chatUsers = conversationRecordRepository.countDistinctUserByMessageTypeBetween(ConversationRecord.MessageType.USER, start, end); } catch (Exception ignored) {}
        try { nutritionUsers = userEventRecordRepository.countDistinctUserByEventTypeBetween("NUTRITION_DAY_SAVE", start, end); } catch (Exception ignored) {}
        try { vipPaidUsers = vipOrderRepo.countDistinctPaidUsersBetween(start, end); } catch (Exception ignored) {}

        List<Map<String, Object>> out = new ArrayList<>();
        out.add(Map.of("stage", "loginUsers", "value", loginUsers));
        out.add(Map.of("stage", "chatUsers", "value", chatUsers));
        out.add(Map.of("stage", "nutritionUsers", "value", nutritionUsers));
        out.add(Map.of("stage", "vipPaidUsers", "value", vipPaidUsers));

        writeAudit(admin, req, "ANALYSIS_FUNNEL", "ANALYSIS", "", "success", Map.of("days", safeDays));
        return ApiResp.ok(out);
    }

    @GetMapping("/analysis/funnelTrend")
    public ApiResp<Map<String, Object>> analysisFunnelTrend(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeDays = Math.max(1, Math.min(days, 365));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> loginUsersByDay = new HashMap<>();
        Map<LocalDate, Long> chatUsersByDay = new HashMap<>();
        Map<LocalDate, Long> nutritionUsersByDay = new HashMap<>();
        Map<LocalDate, Long> vipPaidUsersByDay = new HashMap<>();

        try {
            List<Object[]> rows = loginRecordRepo.dailyDauBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                loginUsersByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = conversationRecordRepository.dailyDistinctUserByMessageTypeBetween(ConversationRecord.MessageType.USER, start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                chatUsersByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = userEventRecordRepository.dailyDistinctUserByEventTypeBetween("NUTRITION_DAY_SAVE", start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                nutritionUsersByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }
        try {
            List<Object[]> rows = vipOrderRepo.dailyDistinctPaidUsersBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                LocalDate d = toLocalDate(r[0]);
                if (d == null) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                vipPaidUsersByDay.put(d, Math.max(0, v));
            }
        } catch (Exception ignored) {
        }

        List<String> dates = new ArrayList<>();
        List<Long> loginUsers = new ArrayList<>();
        List<Long> chatUsers = new ArrayList<>();
        List<Long> nutritionUsers = new ArrayList<>();
        List<Long> vipPaidUsers = new ArrayList<>();

        for (int i = 0; i < safeDays; i++) {
            LocalDate d = startDate.plusDays(i);
            dates.add(d.toString());
            loginUsers.add(loginUsersByDay.getOrDefault(d, 0L));
            chatUsers.add(chatUsersByDay.getOrDefault(d, 0L));
            nutritionUsers.add(nutritionUsersByDay.getOrDefault(d, 0L));
            vipPaidUsers.add(vipPaidUsersByDay.getOrDefault(d, 0L));
        }

        Map<String, Object> out = new HashMap<>();
        out.put("rangeDays", safeDays);
        out.put("dates", dates);
        out.put("loginUsers", loginUsers);
        out.put("chatUsers", chatUsers);
        out.put("nutritionUsers", nutritionUsers);
        out.put("vipPaidUsers", vipPaidUsers);

        writeAudit(admin, req, "ANALYSIS_FUNNEL_TREND", "ANALYSIS", "", "success", Map.of("days", safeDays));
        return ApiResp.ok(out);
    }

    @GetMapping("/analysis/retention")
    public ApiResp<Map<String, Object>> analysisRetention(
            @RequestParam(defaultValue = "14") int cohortDays,
            @RequestParam(defaultValue = "7") int retentionDays,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeCohortDays = Math.max(1, Math.min(cohortDays, 60));
        int safeRetentionDays = Math.max(1, Math.min(retentionDays, 30));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeCohortDays - 1L);

        List<String> cohorts = new ArrayList<>();
        List<Integer> offsets = new ArrayList<>();
        for (int i = 1; i <= safeRetentionDays; i++) offsets.add(i);

        List<Long> cohortSizes = new ArrayList<>();
        List<List<Long>> retainedUsers = new ArrayList<>();

        for (int i = 0; i < safeCohortDays; i++) {
            LocalDate cohortDate = startDate.plusDays(i);
            cohorts.add(cohortDate.toString());
            LocalDateTime cohortStart = cohortDate.atStartOfDay();
            LocalDateTime cohortEnd = cohortDate.plusDays(1).atStartOfDay();

            long cohortSize = 0;
            try { cohortSize = userRepo.countCreatedAtBetween(cohortStart, cohortEnd); } catch (Exception ignored) {}
            cohortSizes.add(Math.max(0, cohortSize));

            List<Long> row = new ArrayList<>();
            for (int k = 1; k <= safeRetentionDays; k++) {
                LocalDate activeDate = cohortDate.plusDays(k);
                if (activeDate.isAfter(LocalDate.now())) {
                    row.add(null);
                    continue;
                }
                LocalDateTime activeStart = activeDate.atStartOfDay();
                LocalDateTime activeEnd = activeDate.plusDays(1).atStartOfDay();
                long retained = 0;
                try {
                    retained = loginRecordRepo.countDistinctCohortActiveUsersBetween(cohortStart, cohortEnd, activeStart, activeEnd);
                } catch (Exception ignored) {
                }
                row.add(Math.max(0, retained));
            }
            retainedUsers.add(row);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("cohortDays", safeCohortDays);
        out.put("retentionDays", safeRetentionDays);
        out.put("cohorts", cohorts);
        out.put("offsets", offsets);
        out.put("cohortSizes", cohortSizes);
        out.put("retainedUsers", retainedUsers);

        writeAudit(admin, req, "ANALYSIS_RETENTION", "ANALYSIS", "", "success", Map.of("cohortDays", safeCohortDays, "retentionDays", safeRetentionDays));
        return ApiResp.ok(out);
    }

    @GetMapping("/analysis/verify")
    public ApiResp<List<Map<String, Object>>> analysisVerify(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int safeDays = Math.max(1, Math.min(days, 365));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long totalNewUsers = 0;
        long sumNewUsers = 0;
        long totalSessions = 0;
        long sumSessions = 0;
        long totalChatUserMsgs = 0;
        long sumChatUserMsgs = 0;
        long totalNutritionDaySaves = 0;
        long sumNutritionDaySaves = 0;
        long totalVipPaidOrders = 0;
        long sumVipPaidOrders = 0;
        BigDecimal totalVipRevenue = BigDecimal.ZERO;
        BigDecimal sumVipRevenue = BigDecimal.ZERO;

        try { totalNewUsers = userRepo.countCreatedAtBetween(start, end); } catch (Exception ignored) {}
        try {
            List<Object[]> rows = userRepo.dailyNewUsersBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                sumNewUsers += Math.max(0, v);
            }
        } catch (Exception ignored) {
        }

        try { totalSessions = loginRecordRepo.countSuccessBetween(start, end); } catch (Exception ignored) {}
        try {
            List<Object[]> rows = loginRecordRepo.dailySessionsBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                sumSessions += Math.max(0, v);
            }
        } catch (Exception ignored) {
        }

        try { totalChatUserMsgs = conversationRecordRepository.countAllByMessageTypeBetween(ConversationRecord.MessageType.USER, start, end); } catch (Exception ignored) {}
        try {
            List<Object[]> rows = conversationRecordRepository.dailyCountByMessageTypeBetween(ConversationRecord.MessageType.USER, start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                sumChatUserMsgs += Math.max(0, v);
            }
        } catch (Exception ignored) {
        }

        try { totalNutritionDaySaves = userEventRecordRepository.countByEventTypeAndOccurredAtBetween("NUTRITION_DAY_SAVE", start, end); } catch (Exception ignored) {}
        try {
            List<Object[]> rows = userEventRecordRepository.dailyCountByEventTypeBetween("NUTRITION_DAY_SAVE", start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                long v = (r[1] instanceof Number n) ? n.longValue() : 0L;
                sumNutritionDaySaves += Math.max(0, v);
            }
        } catch (Exception ignored) {
        }

        try { totalVipPaidOrders = vipOrderRepo.countPaidBetween(start, end); } catch (Exception ignored) {}
        try { totalVipRevenue = vipOrderRepo.sumPaidAmountCnyBetween(start, end); } catch (Exception ignored) {}
        if (totalVipRevenue == null) totalVipRevenue = BigDecimal.ZERO;
        try {
            List<Object[]> rows = vipOrderRepo.dailyPaidStatsBetween(start, end);
            for (Object[] r : rows) {
                if (r == null || r.length < 3) continue;
                long cnt = (r[1] instanceof Number n) ? n.longValue() : 0L;
                BigDecimal amt = (r[2] instanceof BigDecimal bd) ? bd : BigDecimal.ZERO;
                sumVipPaidOrders += Math.max(0, cnt);
                if (amt != null) sumVipRevenue = sumVipRevenue.add(amt);
            }
        } catch (Exception ignored) {
        }

        List<Map<String, Object>> out = new ArrayList<>();
        out.add(checkRow("newUsers_total_vs_dailySum", totalNewUsers, sumNewUsers));
        out.add(checkRow("sessions_total_vs_dailySum", totalSessions, sumSessions));
        out.add(checkRow("chatUserMsgs_total_vs_dailySum", totalChatUserMsgs, sumChatUserMsgs));
        out.add(checkRow("nutritionSaves_total_vs_dailySum", totalNutritionDaySaves, sumNutritionDaySaves));
        out.add(checkRow("vipPaidOrders_total_vs_dailySum", totalVipPaidOrders, sumVipPaidOrders));
        out.add(checkRow("vipRevenue_total_vs_dailySum", totalVipRevenue, sumVipRevenue));

        writeAudit(admin, req, "ANALYSIS_VERIFY", "ANALYSIS", "", "success", Map.of("days", safeDays));
        return ApiResp.ok(out);
    }

    private static Map<String, Object> checkRow(String name, long expected, long actual) {
        long diff = expected - actual;
        return Map.of(
                "name", name,
                "expected", expected,
                "actual", actual,
                "diff", diff,
                "ok", diff == 0
        );
    }

    private static Map<String, Object> checkRow(String name, BigDecimal expected, BigDecimal actual) {
        BigDecimal e = expected == null ? BigDecimal.ZERO : expected;
        BigDecimal a = actual == null ? BigDecimal.ZERO : actual;
        BigDecimal diff = e.subtract(a);
        return Map.of(
                "name", name,
                "expected", e,
                "actual", a,
                "diff", diff,
                "ok", diff.compareTo(BigDecimal.ZERO) == 0
        );
    }

    private static LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate d) return d;
        if (v instanceof java.sql.Date d) return d.toLocalDate();
        if (v instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (v instanceof java.util.Date dt) {
            return dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;
            if (s.length() >= 10) s = s.substring(0, 10);
            return LocalDate.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    @GetMapping("/admins")
    public ApiResp<PageResp<Map<String, Object>>> admins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "false") boolean onlyActive,
            @RequestParam(required = false) String account,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int lvl = roleLevel(admin);
        if (lvl >= 3) {
            writeAudit(admin, req, "ADMIN_LIST", "ADMIN", "", "failed", Map.of("reason", "forbidden"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
        }

        Integer minRoleLevel = (lvl == 1) ? null : 2;
        Integer maxRoleLevel = null;
        String bizLine = "";
        Long createdByAdminId = null;

        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        Page<AdminUser> pg = adminUserRepo.pageAdmin(
                safeStr(account),
                onlyActive,
                minRoleLevel,
                maxRoleLevel,
                bizLine,
                createdByAdminId,
                admin.getId(),
                pageable
        );
        List<Map<String, Object>> list = pg.getContent().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("uid", safeStr(a.getUid()));
            m.put("username", safeStr(a.getUsername()));
            m.put("nickname", safeStr(a.getNickname()));
            m.put("phoneNumber", safeStr(a.getPhoneNumber()));
            m.put("email", safeStr(a.getEmail()));
            m.put("avatarUrl", safeStr(a.getAvatarUrl()));
            m.put("roleLevel", roleLevel(a));
            String bz = normalizeBizLine(a.getBizLine());
            m.put("bizLine", bz.isEmpty() ? "all" : bz);
            m.put("status", a.getStatus());
            m.put("lastLoginAt", formatDateTime(a.getLastLoginAt()));
            m.put("failedLoginCount", a.getFailedLoginCount() == null ? 0 : a.getFailedLoginCount());
            m.put("lockedUntil", formatDateTime(a.getLockedUntil()));
            m.put("createTime", formatDateTime(a.getCreatedAt()));
            return m;
        }).toList();

        Map<String, Object> detail = new HashMap<>();
        detail.put("page", page);
        detail.put("pageSize", pageSize);
        detail.put("onlyActive", onlyActive);
        detail.put("account", safeStr(account));
        detail.put("scopeMinRoleLevel", minRoleLevel);
        detail.put("scopeBizLine", bizLine);
        detail.put("scopeCreatedByAdminId", createdByAdminId);
        writeAudit(admin, req, "ADMIN_LIST", "ADMIN", "", "success", detail);
        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    @PostMapping("/admins/save")
    public ApiResp<Map<String, Object>> saveAdmin(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = currentAdmin();
        int lvl = roleLevel(operator);
        if (lvl >= 3) {
            writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "forbidden"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
        }
        if (body == null) body = Map.of();

        Long id = parseLongOrNull(body.get("id"));
        String username = safeStr(body.get("username"));
        String nickname = safeStr(body.get("nickname"));
        String email = safeStr(body.get("email"));
        Integer status = parseIntOrNull(body.get("status"));
        Integer targetRoleLevel = parseIntOrNull(body.get("roleLevel"));
        String rawBizLine = safeStr(body.get("bizLine"));
        String rawDeptBizLine = extractDepartmentId(body);
        String rawSelectedBizLine = !rawDeptBizLine.isEmpty() ? rawDeptBizLine : rawBizLine;
        boolean selectedAll = "all".equalsIgnoreCase(rawSelectedBizLine.trim());
        String bizLineFromBody = normalizeBizLine(rawSelectedBizLine);
        boolean bizLineSpecified = body.containsKey("bizLine")
                || body.containsKey("department")
                || body.containsKey("department.id")
                || body.containsKey("departmentId");
        if (lvl == 2 && bizLineSpecified && bizLineFromBody.isEmpty()) {
            selectedAll = true;
        }

        LocalDateTime now = LocalDateTime.now();

        if (id == null) {
            int rl = targetRoleLevel == null ? 2 : targetRoleLevel;
            if (rl <= 1) {
                writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "invalid_role_level"));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不允许创建一级管理员");
            }
            if (rl != 2 && rl != 3) {
                writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "invalid_role_level"));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "级别不合法");
            }

            String bizLine = bizLineFromBody;
            if (lvl == 2) {
                String myBiz = normalizeBizLine(safeStr(operator.getBizLine()));
                if (!myBiz.isEmpty()) {
                    if (rl == 3) {
                        bizLine = myBiz;
                    } else if (bizLine.isEmpty()) {
                        bizLine = myBiz;
                    }
                } else {
                    if (rl == 3) {
                        if (bizLine.isEmpty()) {
                            writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "missing_bizLine"));
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需绑定业务线");
                        }
                    } else {
                        if (selectedAll) {
                            bizLine = "";
                        } else if (bizLine.isEmpty()) {
                            writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "missing_bizLine"));
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需绑定业务线");
                        }
                    }
                }
            } else if (lvl == 1 && selectedAll) {
                bizLine = "";
            }
            if (bizLine.isEmpty() && !((lvl == 1 && selectedAll) || (lvl == 2 && selectedAll && rl == 2))) {
                writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "missing_bizLine"));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需绑定业务线");
            }

            if (username.isEmpty()) {
                writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "missing_username"));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入账号名");
            }
            if (adminUserRepo.findByUsername(username).isPresent()) {
                writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "duplicate_username"));
                throw new ResponseStatusException(HttpStatus.CONFLICT, "账号名已存在");
            }
            if (!email.isEmpty() && adminUserRepo.findByEmail(email).isPresent()) {
                writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "duplicate_email"));
                throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已存在");
            }

            String pwd = safeStr(body.get("password"));
            boolean generated = false;
            if (pwd.isEmpty()) {
                pwd = genStrongPassword();
                generated = true;
            }
            if (!isStrongPassword(pwd)) {
                writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", "", "failed", Map.of("reason", "weak_password"));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码强度不足");
            }

            AdminUser a = new AdminUser();
            a.setUid(genUniqueAdminUid());
            a.setUsername(username);
            a.setNickname(nickname.isEmpty() ? username : nickname);
            if (!email.isEmpty()) a.setEmail(email);
            a.setRole("ADMIN");
            a.setRoleLevel(rl);
            a.setBizLine(bizLine);
            a.setCreatedByAdminId(operator.getId());
            a.setStatus(status == null ? 1 : status);
            a.setPasswordHash(passwordEncoder.encode(pwd));
            a.setFailedLoginCount(0);
            a.setLockedUntil(null);
            a.setCreatedAt(now);
            a.setUpdatedAt(now);
            adminUserRepo.save(a);

            Map<String, Object> out = new HashMap<>();
            out.put("success", true);
            out.put("id", a.getId());
            if (generated) out.put("generatedPassword", pwd);
            writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", String.valueOf(a.getId()), "success", Map.of("op", "create", "roleLevel", rl, "bizLine", bizLine, "generatedPassword", generated));
            return ApiResp.ok(out);
        }

        AdminUser target = adminUserRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "管理员不存在"));
        assertCanManageAdmin(operator, target);
        if (!nickname.isEmpty()) target.setNickname(nickname);

        if (!email.isEmpty()) {
            var existed = adminUserRepo.findByEmail(email).orElse(null);
            if (existed != null && !existed.getId().equals(target.getId())) {
                writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "duplicate_email"));
                throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已存在");
            }
            target.setEmail(email);
        }

        if (status != null) target.setStatus(status);
        if (lvl <= 2) {
            int effectiveTargetRoleLevel = roleLevel(target);
            if (targetRoleLevel != null) {
                if (operator.getId() != null && operator.getId().equals(target.getId())) {
                    writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "self_role_change_forbidden"));
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可修改自身级别");
                }
                int rl = targetRoleLevel;
                if (rl != 2 && rl != 3) {
                    writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "invalid_role_level"));
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "级别不合法");
                }
                target.setRoleLevel(rl);
                effectiveTargetRoleLevel = rl;
            }
            boolean shouldEvaluateBizLine = bizLineSpecified || targetRoleLevel != null;
            if (shouldEvaluateBizLine) {
                String bizLine = bizLineFromBody.isEmpty() ? normalizeBizLine(target.getBizLine()) : bizLineFromBody;
                if (lvl == 2) {
                    String myBiz = normalizeBizLine(safeStr(operator.getBizLine()));
                    if (!myBiz.isEmpty()) {
                        if (effectiveTargetRoleLevel == 3) {
                            bizLine = myBiz;
                        } else if (bizLineFromBody.isEmpty()) {
                            bizLine = myBiz;
                        }
                    } else {
                        if (effectiveTargetRoleLevel == 3 && bizLine.isEmpty()) {
                            writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "missing_bizLine"));
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需绑定业务线");
                        }
                        if (selectedAll && effectiveTargetRoleLevel == 2) {
                            bizLine = "";
                        }
                    }
                } else if (lvl == 1 && selectedAll) {
                    bizLine = "";
                }
                if (bizLine.isEmpty() && !((lvl == 1 && selectedAll) || (lvl == 2 && selectedAll && effectiveTargetRoleLevel == 2))) {
                    writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "missing_bizLine"));
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需绑定业务线");
                }
                target.setBizLine(bizLine);
            }
        }

        target.setUpdatedAt(now);
        adminUserRepo.save(target);
        writeAudit(operator, req, "ADMIN_SAVE", "ADMIN", String.valueOf(target.getId()), "success", Map.of("op", "update"));
        return ApiResp.ok(Map.of("success", true));
    }

    @PostMapping("/admins/batch-update")
    public ApiResp<Map<String, Object>> batchUpdateAdmins(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = currentAdmin();
        int lvl = roleLevel(operator);
        if (lvl >= 3) {
            writeAudit(operator, req, "ADMIN_BATCH_UPDATE_APPROVAL", "ADMIN", "", "failed", Map.of("reason", "forbidden"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
        }
        Object idsObj = body == null ? null : body.get("ids");
        if (!(idsObj instanceof List<?> ids) || ids.size() < 2) {
            writeAudit(operator, req, "ADMIN_BATCH_UPDATE_APPROVAL", "ADMIN", "", "failed", Map.of("reason", "invalid_ids"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "批量修改至少选择2个账号");
        }

        List<Long> adminIds = new ArrayList<>();
        for (Object it : ids) {
            Long v = parseLongOrNull(it);
            if (v != null) adminIds.add(v);
        }
        if (adminIds.size() < 2) {
            writeAudit(operator, req, "ADMIN_BATCH_UPDATE_APPROVAL", "ADMIN", "", "failed", Map.of("reason", "invalid_ids"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "批量修改至少选择2个账号");
        }

        List<AdminUser> targets = adminUserRepo.findAllById(adminIds);
        for (AdminUser t : targets) {
            assertCanManageAdmin(operator, t);
        }

        AdminAuditLog l = new AdminAuditLog();
        l.setOperatorAdminId(operator.getId());
        l.setOperatorAdminUid(safeStr(operator.getUid()));
        l.setOperatorRoleLevel(operator.getRoleLevel());
        l.setOperatorBizLine(safeStr(operator.getBizLine()));
        l.setAction("ADMIN_BATCH_UPDATE_APPROVAL");
        l.setTargetType("ADMIN");
        l.setTargetId(String.valueOf(adminIds));
        l.setIp(clientIp(req));
        l.setResult("success");
        l.setDetail(String.valueOf(body == null ? Map.of() : body));
        l.setCreatedAt(LocalDateTime.now());
        adminAuditLogRepo.save(l);
        return ApiResp.ok(Map.of("success", true, "approvalId", l.getId()));
    }

    @Transactional
    @PostMapping("/admins/delete")
    public ApiResp<Map<String, Object>> deleteAdmins(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = currentAdmin();
        int lvl = roleLevel(operator);
        if (lvl >= 3) {
            writeAudit(operator, req, "ADMIN_DELETE", "ADMIN", "", "failed", Map.of("reason", "forbidden"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
        }

        Object idsObj = body == null ? null : body.get("ids");
        if (!(idsObj instanceof List<?> ids) || ids.isEmpty()) {
            writeAudit(operator, req, "ADMIN_DELETE", "ADMIN", "", "failed", Map.of("reason", "missing_ids"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要删除的数据");
        }

        List<Long> adminIds = new ArrayList<>();
        for (Object it : ids) {
            Long v = parseLongOrNull(it);
            if (v != null) adminIds.add(v);
        }
        if (adminIds.isEmpty()) {
            writeAudit(operator, req, "ADMIN_DELETE", "ADMIN", "", "failed", Map.of("reason", "invalid_ids"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要删除的数据");
        }

        List<AdminUser> targets = adminUserRepo.findAllById(adminIds);
        for (AdminUser t : targets) {
            assertCanManageAdmin(operator, t);
            if (t.getId() != null && userRepo.existsActiveUsersCreatedBy(t.getId())) {
                writeAudit(operator, req, "ADMIN_DELETE", "ADMIN", String.valueOf(t.getId()), "failed", Map.of("reason", "active_users_exist"));
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该账号仍有未交接成员");
            }
            if (t.getId() != null && hasPendingBusinessForAdmin(t.getId())) {
                writeAudit(operator, req, "ADMIN_DELETE", "ADMIN", String.valueOf(t.getId()), "failed", Map.of("reason", "pending_business"));
                throw new ResponseStatusException(HttpStatus.CONFLICT, "存在未完成业务，禁止删除");
            }
        }

        boolean hardDelete = body == null || body.get("hardDelete") == null || Boolean.parseBoolean(safeStr(body.get("hardDelete")));
        if (hardDelete) {
            adminRefreshTokenRepo.deleteByAdminUser_IdIn(adminIds);
            adminUserRepo.deleteAllByIdInBatch(adminIds);
            writeAudit(operator, req, "ADMIN_DELETE", "ADMIN", String.valueOf(adminIds), "success", Map.of("ids", adminIds, "hardDelete", true));
        } else {
            LocalDateTime now = LocalDateTime.now();
            for (AdminUser t : targets) {
                t.setStatus(0);
                t.setUpdatedAt(now);
            }
            adminUserRepo.saveAll(targets);
            writeAudit(operator, req, "ADMIN_DELETE", "ADMIN", String.valueOf(adminIds), "success", Map.of("ids", adminIds, "hardDelete", false));
        }
        return ApiResp.ok(Map.of("success", true));
    }

    @PostMapping("/admins/reset-password")
    public ApiResp<Map<String, Object>> resetAdminPassword(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = currentAdmin();
        int lvl = roleLevel(operator);
        if (lvl >= 3) {
            writeAudit(operator, req, "ADMIN_RESET_PWD", "ADMIN", "", "failed", Map.of("reason", "forbidden"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
        }
        if (body == null) body = Map.of();
        Long id = parseLongOrNull(body.get("id"));
        if (id == null) {
            writeAudit(operator, req, "ADMIN_RESET_PWD", "ADMIN", "", "failed", Map.of("reason", "missing_id"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少管理员ID");
        }
        String pwd = safeStr(body.get("password"));
        if (!isStrongPassword(pwd)) {
            writeAudit(operator, req, "ADMIN_RESET_PWD", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "weak_password"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码强度不足");
        }

        AdminUser target = adminUserRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "管理员不存在"));
        assertCanManageAdmin(operator, target);
        if (passwordEncoder == null) {
            writeAudit(operator, req, "ADMIN_RESET_PWD", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "password_encoder_missing"));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "密码服务未初始化");
        }
        try {
            target.setPasswordHash(passwordEncoder.encode(pwd));
        } catch (Exception e) {
            writeAudit(operator, req, "ADMIN_RESET_PWD", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "encode_failed"));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "重置密码失败");
        }
        target.setFailedLoginCount(0);
        target.setLockedUntil(null);
        target.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(target);
        writeAudit(operator, req, "ADMIN_RESET_PWD", "ADMIN", String.valueOf(id), "success", Map.of("op", "reset"));
        return ApiResp.ok(Map.of("success", true));
    }

    @PostMapping("/admins/change-password")
    public ApiResp<Map<String, Object>> changeAdminPassword(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = currentAdmin();
        if (body == null) body = Map.of();
        String oldPwd = safeStr(body.get("oldPassword"));
        String newPwd = safeStr(body.get("newPassword"));
        if (!isStrongPassword(newPwd)) {
            writeAudit(operator, req, "ADMIN_CHANGE_PWD", "ADMIN", String.valueOf(operator.getId()), "failed", Map.of("reason", "weak_password"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码强度不足");
        }

        AdminUser me = adminUserRepo.findById(operator.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
        String hash = safeStr(me.getPasswordHash());
        if (hash.isEmpty() || !passwordEncoder.matches(oldPwd, hash)) {
            writeAudit(operator, req, "ADMIN_CHANGE_PWD", "ADMIN", String.valueOf(operator.getId()), "failed", Map.of("reason", "wrong_old_password"));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "原密码错误");
        }
        me.setPasswordHash(passwordEncoder.encode(newPwd));
        me.setFailedLoginCount(0);
        me.setLockedUntil(null);
        me.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(me);
        writeAudit(operator, req, "ADMIN_CHANGE_PWD", "ADMIN", String.valueOf(operator.getId()), "success", Map.of("op", "self_change"));
        return ApiResp.ok(Map.of("success", true));
    }

    @PostMapping("/admins/unlock")
    public ApiResp<Map<String, Object>> unlockAdmin(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = currentAdmin();
        int lvl = roleLevel(operator);
        if (lvl != 1) {
            writeAudit(operator, req, "ADMIN_UNLOCK", "ADMIN", "", "failed", Map.of("reason", "forbidden"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
        }
        if (body == null) body = Map.of();
        Long id = parseLongOrNull(body.get("id"));
        if (id == null) {
            writeAudit(operator, req, "ADMIN_UNLOCK", "ADMIN", "", "failed", Map.of("reason", "missing_id"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少管理员ID");
        }
        AdminUser target = adminUserRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "管理员不存在"));
        int tl = roleLevel(target);
        if (tl == 1 && operator.getId() != null && operator.getId().equals(target.getId())) {
            writeAudit(operator, req, "ADMIN_UNLOCK", "ADMIN", String.valueOf(id), "failed", Map.of("reason", "self_unlock_forbidden"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可解锁自身");
        }
        target.setFailedLoginCount(0);
        target.setLockedUntil(null);
        target.setUpdatedAt(LocalDateTime.now());
        adminUserRepo.save(target);
        writeAudit(operator, req, "ADMIN_UNLOCK", "ADMIN", String.valueOf(id), "success", Map.of("op", "unlock"));
        return ApiResp.ok(Map.of("success", true));
    }

    @GetMapping("/overview")
    public ApiResp<Map<String, Object>> overview(@RequestParam(defaultValue = "7") int days) {
        int d = Math.max(1, Math.min(30, days));
        LocalDateTime after = LocalDateTime.now().minusDays(d);

        long usersTotal = userRepo.count();
        long usersNew = userRepo.countCreatedAtAfter(after);
        long usersActive = userRepo.countActiveUsersLoggedInAfter(after);

        BigDecimal rechargePaidCny = safe(paymentOrderRepo.sumPaidAmountCnyAfter(after));
        BigDecimal vipPaidCny = safe(vipOrderRepo.sumPaidAmountCnyAfter(after));
        BigDecimal spendKc = safe(spendOrderRepo.sumAmountKcAfter(after));

        Map<String, Object> out = new HashMap<>();
        out.put("days", d);
        out.put("after", after.toString());
        out.put("usersTotal", usersTotal);
        out.put("usersNew", usersNew);
        out.put("usersActive", usersActive);
        out.put("rechargePaidCny", rechargePaidCny);
        out.put("vipPaidCny", vipPaidCny);
        out.put("spendKc", spendKc);
        return ApiResp.ok(out);
    }

    @GetMapping("/users")
    public ApiResp<PageResp<Map<String, Object>>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "false") boolean onlyActive,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) String id,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int lvl = roleLevel(admin);
        String bizLine = resolveBizLineForList(admin, lvl, safeStr(id));
        Long createdByAdminId = resolveCreatorForList(admin, lvl);
        boolean onlyManaged = lvl >= 3;

        Map<String, Object> detail = new HashMap<>();
        detail.put("page", page);
        detail.put("pageSize", pageSize);
        detail.put("onlyActive", onlyActive);
        detail.put("username", safeStr(username));
        detail.put("account", safeStr(account));
        detail.put("onlyManaged", onlyManaged);
        detail.put("bizLine", bizLine);
        detail.put("createdByAdminId", createdByAdminId);
        writeAudit(admin, req, "USER_LIST", "USER", "", "success", detail);
        return ApiResp.ok(adminUserService.listUsers(page, pageSize, onlyActive, username, account, id));
    }

    @PostMapping("/users/reset-password")
    public ApiResp<Map<String, Object>> resetUserPassword(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser operator = currentAdmin();
        try {
            adminUserService.resetUserPassword(body);
            Long id = parseLongOrNull(body == null ? null : body.get("id"));
            writeAudit(operator, req, "USER_RESET_PWD", "USER", id == null ? "" : String.valueOf(id), "success", Map.of("op", "reset"));
            return ApiResp.ok(Map.of("success", true));
        } catch (ResponseStatusException e) {
            Long id = parseLongOrNull(body == null ? null : body.get("id"));
            writeAudit(operator, req, "USER_RESET_PWD", "USER", id == null ? "" : String.valueOf(id), "failed", Map.of("reason", safeStr(e.getReason())));
            throw e;
        }
    }

    @Transactional
    @PostMapping("/users/delete")
    public ApiResp<Map<String, Object>> deleteUsers(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        Object idsObj = body == null ? null : body.get("ids");
        List<Long> userIds = new ArrayList<>();
        if (idsObj instanceof List<?> ids) {
            for (Object it : ids) {
                Long v = parseLongOrNull(it);
                if (v != null) userIds.add(v);
            }
        }
        try {
            adminUserService.hardDeleteUsers(body);
            writeAudit(admin, req, "USER_DELETE", "USER", String.valueOf(userIds), "success", Map.of("ids", userIds, "hardDelete", true));
            return ApiResp.ok(Map.of("success", true));
        } catch (ResponseStatusException e) {
            writeAudit(admin, req, "USER_DELETE", "USER", String.valueOf(userIds), "failed", Map.of("reason", safeStr(e.getReason())));
            throw e;
        }
    }

    @PostMapping("/users/save")
    public ApiResp<Map<String, Object>> saveUser(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        int lvl = roleLevel(admin);
        if (body == null) body = Map.of();

        Long id = parseLongOrNull(body.get("id"));
        String username = safeStr(body.get("username"));
        String account = safeStr(body.get("account"));
        String email = safeStr(body.get("email"));
        Integer status = parseIntOrNull(body.get("status"));
        String bizLineFromBody = safeStr(extractDepartmentId(body));

        LocalDateTime now = LocalDateTime.now();

        if (id == null) {
            if (lvl == 2) {
                String myBiz = safeStr(admin.getBizLine());
                if (myBiz.isEmpty()) {
                    if (bizLineFromBody.isEmpty() || "all".equalsIgnoreCase(bizLineFromBody)) {
                        writeAudit(admin, req, "USER_SAVE", "USER", "", "failed", Map.of("reason", "missing_bizLine"));
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需绑定业务线");
                    }
                } else {
                    if (bizLineFromBody.isEmpty() || "all".equalsIgnoreCase(bizLineFromBody)) bizLineFromBody = myBiz;
                    if (!myBiz.equals(bizLineFromBody)) {
                        writeAudit(admin, req, "USER_SAVE", "USER", "", "failed", Map.of("reason", "cross_bizLine"));
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可创建非本业务线成员");
                    }
                }
            } else if (lvl >= 3) {
                String myBiz = safeStr(admin.getBizLine());
                if (myBiz.isEmpty()) {
                    writeAudit(admin, req, "USER_SAVE", "USER", "", "failed", Map.of("reason", "missing_bizLine"));
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "未配置业务线");
                }
                if (bizLineFromBody.isEmpty() || "all".equalsIgnoreCase(bizLineFromBody)) bizLineFromBody = myBiz;
                if (!myBiz.equals(bizLineFromBody)) {
                    writeAudit(admin, req, "USER_SAVE", "USER", "", "failed", Map.of("reason", "cross_bizLine"));
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可创建非本业务线成员");
                }
            }

            User u = new User();
            u.setUid(genUniqueUid(account));
            u.setNickname(username.isEmpty() ? account : username);
            applyAccount(u, account, email);
            u.setRole("USER");
            u.setBizLine(normalizeBizLine(bizLineFromBody));
            u.setCreatedByAdminId(admin.getId());
            u.setStatus(status == null ? 1 : status);
            u.setCreatedAt(now);
            u.setUpdatedAt(now);
            u.setPasswordHash(passwordEncoder.encode("123456"));
            userRepo.save(u);
            writeAudit(admin, req, "USER_SAVE", "USER", String.valueOf(u.getId()), "success", Map.of("op", "create", "bizLine", safeStr(u.getBizLine())));
            return ApiResp.ok(Map.of("success", true, "id", String.valueOf(u.getId())));
        }

        User u = userRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (lvl == 2) {
            String myBiz = safeStr(admin.getBizLine());
            if (!myBiz.isEmpty()) {
                String ub = safeStr(u.getBizLine());
                if (!ub.isEmpty() && !myBiz.equals(ub)) {
                    writeAudit(admin, req, "USER_SAVE", "USER", String.valueOf(id), "failed", Map.of("reason", "cross_bizLine"));
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可修改非本业务线成员");
                }
            }
        } else if (lvl >= 3) {
            if (u.getCreatedByAdminId() == null || admin.getId() == null || !admin.getId().equals(u.getCreatedByAdminId())) {
                writeAudit(admin, req, "USER_SAVE", "USER", String.valueOf(id), "failed", Map.of("reason", "not_owner"));
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅可修改自己创建的成员");
            }
        }

        if (!username.isEmpty()) u.setNickname(username);
        applyAccount(u, account, email);
        if (lvl <= 2) {
            if (status != null) u.setStatus(status);
            if (!bizLineFromBody.isEmpty() && !"all".equalsIgnoreCase(bizLineFromBody)) {
                if (lvl == 2) {
                    String myBiz = safeStr(admin.getBizLine());
                    if (!myBiz.isEmpty() && !myBiz.equals(bizLineFromBody)) {
                        writeAudit(admin, req, "USER_SAVE", "USER", String.valueOf(id), "failed", Map.of("reason", "cross_bizLine"));
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可变更为非本业务线");
                    }
                }
                u.setBizLine(normalizeBizLine(bizLineFromBody));
            }
        }
        u.setUpdatedAt(now);
        userRepo.save(u);
        writeAudit(admin, req, "USER_SAVE", "USER", String.valueOf(u.getId()), "success", Map.of("op", "update", "bizLine", safeStr(u.getBizLine())));
        return ApiResp.ok(Map.of("success", true));
    }

    @GetMapping("/audit/logs")
    public ApiResp<PageResp<Map<String, Object>>> auditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        int lvl = roleLevel(admin);
        Long operatorAdminId = (lvl >= 3) ? admin.getId() : null;
        String bizLine = "";

        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        Page<AdminAuditLog> pg = adminAuditLogRepo.pageByScope(operatorAdminId, bizLine, lvl, admin.getId(), pageable);
        Map<Long, AdminUser> adminById = new HashMap<>();
        List<Long> adminIds = pg.getContent().stream()
                .map(AdminAuditLog::getOperatorAdminId)
                .filter(v -> v != null)
                .distinct()
                .toList();
        if (!adminIds.isEmpty()) {
            for (AdminUser a : adminUserRepo.findAllById(adminIds)) {
                if (a != null && a.getId() != null) adminById.put(a.getId(), a);
            }
        }

        List<Map<String, Object>> list = pg.getContent().stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("operatorAdminId", l.getOperatorAdminId());
            m.put("operatorAdminUid", safeStr(l.getOperatorAdminUid()));
            AdminUser op = l.getOperatorAdminId() == null ? null : adminById.get(l.getOperatorAdminId());
            m.put("operatorNickname", op == null ? "" : safeStr(op.getNickname()));
            m.put("operatorRoleLevel", l.getOperatorRoleLevel());
            m.put("operatorBizLine", safeStr(l.getOperatorBizLine()));
            m.put("action", safeStr(l.getAction()));
            m.put("targetType", safeStr(l.getTargetType()));
            m.put("targetId", safeStr(l.getTargetId()));
            m.put("ip", safeStr(l.getIp()));
            m.put("result", safeStr(l.getResult()));
            m.put("detail", safeStr(l.getDetail()));
            m.put("createdAt", formatDateTime(l.getCreatedAt()));
            return m;
        }).toList();

        writeAudit(admin, req, "AUDIT_LIST", "AUDIT", "", "success", Map.of("page", page, "pageSize", pageSize));
        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    @GetMapping("/recipes")
    public ApiResp<PageResp<Map<String, Object>>> recipes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer isActive,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        assertCanManageRecipeBiz(admin, req, "RECIPE_LIST", "");

        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        Page<Recipe> pg = recipeRepository.pageAdmin(safeStr(q), isActive, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", String.valueOf(r.getId()));
            m.put("title", safeStr(r.getTitle()));
            m.put("imageUrl", safeStr(r.getImageUrl()));
            m.put("calories", r.getCalories() == null ? 0 : Math.max(0, r.getCalories()));
            m.put("timeMinutes", r.getTimeMinutes() == null ? 0 : Math.max(0, r.getTimeMinutes()));
            m.put("carbG", safeDouble(r.getCarbG()));
            m.put("proteinG", safeDouble(r.getProteinG()));
            m.put("fatG", safeDouble(r.getFatG()));
            m.put("fiberG", safeDouble(r.getFiberG()));
            m.put("organKeys", safeStr(r.getOrganKeys()));
            m.put("tags", safeStr(r.getTags()));
            m.put("sortOrder", r.getSortOrder() == null ? 0 : r.getSortOrder());
            m.put("isActive", r.getIsActive() == null ? 0 : r.getIsActive());
            m.put("updatedAt", formatDateTime(r.getUpdatedAt()));
            return m;
        }).toList();

        Map<String, Object> auditDetail = new HashMap<>();
        auditDetail.put("page", page);
        auditDetail.put("pageSize", pageSize);
        auditDetail.put("q", safeStr(q));
        if (isActive != null) auditDetail.put("isActive", isActive);
        writeAudit(admin, req, "RECIPE_LIST", "RECIPE", "", "success", auditDetail);
        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    @GetMapping("/recipes/{id}")
    public ApiResp<Map<String, Object>> recipeDetailAdmin(@PathVariable Long id, HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        assertCanManageRecipeBiz(admin, req, "RECIPE_DETAIL", String.valueOf(id));
        Recipe r = recipeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "不存在"));
        Map<String, Object> out = new HashMap<>();
        out.put("id", String.valueOf(r.getId()));
        out.put("title", safeStr(r.getTitle()));
        out.put("imageUrl", safeStr(r.getImageUrl()));
        out.put("calories", r.getCalories() == null ? 0 : Math.max(0, r.getCalories()));
        out.put("timeMinutes", r.getTimeMinutes() == null ? 0 : Math.max(0, r.getTimeMinutes()));
        out.put("carbG", safeDouble(r.getCarbG()));
        out.put("proteinG", safeDouble(r.getProteinG()));
        out.put("fatG", safeDouble(r.getFatG()));
        out.put("fiberG", safeDouble(r.getFiberG()));
        out.put("defaultReason", safeStr(r.getDefaultReason()));
        out.put("valueText", safeStr(r.getValueText()));
        out.put("organKeys", safeStr(r.getOrganKeys()));
        out.put("tags", safeStr(r.getTags()));
        out.put("sortOrder", r.getSortOrder() == null ? 0 : r.getSortOrder());
        out.put("isActive", r.getIsActive() == null ? 0 : r.getIsActive());
        out.put("ingredients", toIngredientMaps(r.getIngredients()));
        out.put("steps", toStepMaps(r.getSteps()));
        out.put("createdAt", formatDateTime(r.getCreatedAt()));
        out.put("updatedAt", formatDateTime(r.getUpdatedAt()));
        writeAudit(admin, req, "RECIPE_DETAIL", "RECIPE", String.valueOf(id), "success", Map.of());
        return ApiResp.ok(out);
    }

    @PostMapping("/recipes/save")
    public ApiResp<Map<String, Object>> saveRecipe(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        Long id = parseLongOrNull(body == null ? null : body.get("id"));
        assertCanManageRecipeBiz(admin, req, "RECIPE_SAVE", id == null ? "" : String.valueOf(id));
        if (body == null) body = Map.of();

        String title = safeStr(body.get("title"));
        if (title.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能为空");

        LocalDateTime now = LocalDateTime.now();
        Recipe r;
        boolean creating = id == null;
        if (creating) {
            r = new Recipe();
            r.setCreatedAt(now);
        } else {
            r = recipeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "不存在"));
        }

        r.setTitle(title);
        r.setImageUrl(safeStr(body.get("imageUrl")));
        r.setCalories(Math.max(0, parseIntOrZero(body.get("calories"))));
        r.setTimeMinutes(Math.max(0, parseIntOrZero(body.get("timeMinutes"))));
        r.setCarbG(safeDouble(body.get("carbG")));
        r.setProteinG(safeDouble(body.get("proteinG")));
        r.setFatG(safeDouble(body.get("fatG")));
        r.setFiberG(safeDouble(body.get("fiberG")));
        r.setDefaultReason(safeStr(body.get("defaultReason")));
        r.setValueText(safeStr(body.get("valueText")));
        r.setOrganKeys(safeStr(body.get("organKeys")));
        r.setTags(safeStr(body.get("tags")));
        r.setSortOrder(parseIntOrZero(body.get("sortOrder")));
        Integer active = parseIntOrNull(body.get("isActive"));
        if (active != null) r.setIsActive(active);
        r.setIngredients(parseRecipeIngredients(body.get("ingredients")));
        r.setSteps(parseRecipeSteps(body.get("steps")));
        r.setUpdatedAt(now);

        recipeRepository.save(r);
        writeAudit(admin, req, "RECIPE_SAVE", "RECIPE", String.valueOf(r.getId()), "success", Map.of("op", creating ? "create" : "update"));
        return ApiResp.ok(Map.of("success", true, "id", String.valueOf(r.getId())));
    }

    @PostMapping("/recipes/delete")
    public ApiResp<Map<String, Object>> deleteRecipes(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        assertCanManageRecipeBiz(admin, req, "RECIPE_DELETE", "");
        Object idsObj = body == null ? null : body.get("ids");
        if (!(idsObj instanceof List<?> ids) || ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要删除的数据");

        boolean hardDelete = body != null && body.get("hardDelete") != null && Boolean.parseBoolean(safeStr(body.get("hardDelete")));
        List<Long> recipeIds = new ArrayList<>();
        for (Object it : ids) {
            Long v = parseLongOrNull(it);
            if (v != null && v > 0) recipeIds.add(v);
        }
        if (recipeIds.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要删除的数据");

        if (hardDelete) {
            recipeRepository.deleteAllByIdInBatch(recipeIds);
        } else {
            LocalDateTime now = LocalDateTime.now();
            List<Recipe> list = recipeRepository.findAllById(recipeIds);
            for (Recipe r : list) {
                r.setIsActive(0);
                r.setUpdatedAt(now);
            }
            recipeRepository.saveAll(list);
        }
        writeAudit(admin, req, "RECIPE_DELETE", "RECIPE", String.valueOf(recipeIds), "success", Map.of("hardDelete", hardDelete));
        return ApiResp.ok(Map.of("success", true));
    }

    @GetMapping("/food-catalogs")
    public ApiResp<PageResp<Map<String, Object>>> foodCatalogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer isActive,
            HttpServletRequest req
    ) {
        AdminUser admin = currentAdmin();
        assertCanManageRecipeBiz(admin, req, "FOOD_CATALOG_LIST", "");

        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        Page<FoodCatalog> pg = foodCatalogRepository.pageAdmin(safeStr(q), isActive, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", String.valueOf(f.getId()));
            m.put("name", safeStr(f.getName()));
            m.put("keywords", safeStr(f.getKeywords()));
            m.put("mealTags", safeStr(f.getMealTags()));
            m.put("baseUnit", safeStr(f.getBaseUnit()));
            m.put("baseAmount", f.getBaseAmount() == null ? 0.0 : f.getBaseAmount());
            m.put("baseCalorie", f.getBaseCalorie() == null ? 0 : Math.max(0, f.getBaseCalorie()));
            m.put("baseCarb", safeDouble(f.getBaseCarb()));
            m.put("baseProtein", safeDouble(f.getBaseProtein()));
            m.put("baseFat", safeDouble(f.getBaseFat()));
            m.put("baseFiber", safeDouble(f.getBaseFiber()));
            m.put("imgUrl", safeStr(f.getImgUrl()));
            m.put("sortOrder", f.getSortOrder() == null ? 0 : f.getSortOrder());
            m.put("isActive", f.getIsActive() == null ? 0 : f.getIsActive());
            m.put("updatedAt", formatDateTime(f.getUpdatedAt()));
            return m;
        }).toList();

        Map<String, Object> auditDetail = new HashMap<>();
        auditDetail.put("page", page);
        auditDetail.put("pageSize", pageSize);
        auditDetail.put("q", safeStr(q));
        if (isActive != null) auditDetail.put("isActive", isActive);
        writeAudit(admin, req, "FOOD_CATALOG_LIST", "FOOD_CATALOG", "", "success", auditDetail);
        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    @GetMapping("/food-catalogs/{id}")
    public ApiResp<Map<String, Object>> foodCatalogDetailAdmin(@PathVariable Long id, HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        assertCanManageRecipeBiz(admin, req, "FOOD_CATALOG_DETAIL", String.valueOf(id));
        FoodCatalog f = foodCatalogRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "不存在"));
        Map<String, Object> out = new HashMap<>();
        out.put("id", String.valueOf(f.getId()));
        out.put("name", safeStr(f.getName()));
        out.put("keywords", safeStr(f.getKeywords()));
        out.put("mealTags", safeStr(f.getMealTags()));
        out.put("baseUnit", safeStr(f.getBaseUnit()));
        out.put("baseAmount", f.getBaseAmount() == null ? 0.0 : f.getBaseAmount());
        out.put("baseCalorie", f.getBaseCalorie() == null ? 0 : Math.max(0, f.getBaseCalorie()));
        out.put("baseCarb", safeDouble(f.getBaseCarb()));
        out.put("baseProtein", safeDouble(f.getBaseProtein()));
        out.put("baseFat", safeDouble(f.getBaseFat()));
        out.put("baseFiber", safeDouble(f.getBaseFiber()));
        out.put("imgUrl", safeStr(f.getImgUrl()));
        out.put("sortOrder", f.getSortOrder() == null ? 0 : f.getSortOrder());
        out.put("isActive", f.getIsActive() == null ? 0 : f.getIsActive());
        out.put("createdAt", formatDateTime(f.getCreatedAt()));
        out.put("updatedAt", formatDateTime(f.getUpdatedAt()));
        writeAudit(admin, req, "FOOD_CATALOG_DETAIL", "FOOD_CATALOG", String.valueOf(id), "success", Map.of());
        return ApiResp.ok(out);
    }

    @PostMapping("/food-catalogs/save")
    public ApiResp<Map<String, Object>> saveFoodCatalog(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        Long id = parseLongOrNull(body == null ? null : body.get("id"));
        assertCanManageRecipeBiz(admin, req, "FOOD_CATALOG_SAVE", id == null ? "" : String.valueOf(id));
        if (body == null) body = Map.of();

        String name = safeStr(body.get("name"));
        if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名称不能为空");

        LocalDateTime now = LocalDateTime.now();
        FoodCatalog f;
        boolean creating = id == null;
        if (creating) {
            f = new FoodCatalog();
            f.setCreatedAt(now);
        } else {
            f = foodCatalogRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "不存在"));
        }

        f.setName(name);
        f.setKeywords(safeStr(body.get("keywords")));
        f.setMealTags(safeStr(body.get("mealTags")));
        String baseUnit = safeStr(body.get("baseUnit"));
        if (!baseUnit.isEmpty()) f.setBaseUnit(baseUnit);
        f.setBaseAmount(Math.max(0.0, safeDouble(body.get("baseAmount"))));
        f.setBaseCalorie(Math.max(0, parseIntOrZero(body.get("baseCalorie"))));
        f.setBaseCarb(safeDouble(body.get("baseCarb")));
        f.setBaseProtein(safeDouble(body.get("baseProtein")));
        f.setBaseFat(safeDouble(body.get("baseFat")));
        f.setBaseFiber(safeDouble(body.get("baseFiber")));
        f.setImgUrl(safeStr(body.get("imgUrl")));
        f.setSortOrder(parseIntOrZero(body.get("sortOrder")));
        Integer active = parseIntOrNull(body.get("isActive"));
        if (active != null) f.setIsActive(active);
        f.setUpdatedAt(now);

        foodCatalogRepository.save(f);
        writeAudit(admin, req, "FOOD_CATALOG_SAVE", "FOOD_CATALOG", String.valueOf(f.getId()), "success", Map.of("op", creating ? "create" : "update"));
        return ApiResp.ok(Map.of("success", true, "id", String.valueOf(f.getId())));
    }

    @PostMapping("/food-catalogs/delete")
    public ApiResp<Map<String, Object>> deleteFoodCatalogs(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        assertCanManageRecipeBiz(admin, req, "FOOD_CATALOG_DELETE", "");
        Object idsObj = body == null ? null : body.get("ids");
        if (!(idsObj instanceof List<?> ids) || ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要删除的数据");

        boolean hardDelete = body != null && body.get("hardDelete") != null && Boolean.parseBoolean(safeStr(body.get("hardDelete")));
        List<Long> foodIds = new ArrayList<>();
        for (Object it : ids) {
            Long v = parseLongOrNull(it);
            if (v != null && v > 0) foodIds.add(v);
        }
        if (foodIds.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要删除的数据");

        if (hardDelete) {
            foodCatalogRepository.deleteAllByIdInBatch(foodIds);
        } else {
            LocalDateTime now = LocalDateTime.now();
            List<FoodCatalog> list = foodCatalogRepository.findAllById(foodIds);
            for (FoodCatalog f : list) {
                f.setIsActive(0);
                f.setUpdatedAt(now);
            }
            foodCatalogRepository.saveAll(list);
        }
        writeAudit(admin, req, "FOOD_CATALOG_DELETE", "FOOD_CATALOG", String.valueOf(foodIds), "success", Map.of("hardDelete", hardDelete));
        return ApiResp.ok(Map.of("success", true));
    }

    @GetMapping("/department/list")
    public ApiResp<Map<String, Object>> departmentList() {
        currentAdmin();
        List<Map<String, Object>> children = new ArrayList<>();
        children.add(Map.of("id", "recipe", "departmentName", "菜谱"));
        children.add(Map.of("id", "shop", "departmentName", "商城"));
        children.add(Map.of("id", "all", "departmentName", "全部"));

        Map<String, Object> root = new HashMap<>();
        root.put("id", "root");
        root.put("departmentName", "业务线");
        root.put("children", children);

        return ApiResp.ok(Map.of("list", List.of(root)));
    }

    @GetMapping("/role/table")
    public ApiResp<Map<String, Object>> roleTable() {
        currentAdmin();
        String now = formatDateTime(LocalDateTime.now());

        Map<String, Object> admin = new HashMap<>();
        admin.put("id", "1");
        admin.put("roleName", "ADMIN");
        admin.put("status", 1);
        admin.put("createTime", now);
        admin.put("remark", "系统内置");

        Map<String, Object> user = new HashMap<>();
        user.put("id", "2");
        user.put("roleName", "USER");
        user.put("status", 1);
        user.put("createTime", now);
        user.put("remark", "系统内置");

        List<Map<String, Object>> list = List.of(admin, user);
        return ApiResp.ok(Map.of("list", list, "total", list.size()));
    }

    @GetMapping("/role/list")
    public ApiResp<List<Map<String, Object>>> roleList(HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        int lvl = roleLevel(admin);

        List<Map<String, Object>> routes = new ArrayList<>();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("path", "/dashboard");
        dashboard.put("component", "#");
        dashboard.put("redirect", "/dashboard/analysis");
        dashboard.put("name", "Dashboard");
        dashboard.put("meta", Map.of(
                "title", "router.dashboard",
                "icon", "vi-ant-design:dashboard-filled",
                "alwaysShow", true
        ));
        dashboard.put("children", List.of(
                Map.of(
                        "path", "analysis",
                        "component", "views/Dashboard/Analysis",
                        "name", "Analysis",
                        "meta", Map.of("title", "router.analysis")
                ),
                Map.of(
                        "path", "workplace",
                        "component", "views/Dashboard/Workplace",
                        "name", "Workplace",
                        "meta", Map.of("title", "router.workplace")
                )
        ));
        routes.add(dashboard);

        Map<String, Object> authorization = new HashMap<>();
        authorization.put("path", "/authorization");
        authorization.put("component", "#");
        authorization.put("redirect", "/authorization/user");
        authorization.put("name", "Authorization");
        authorization.put("meta", Map.of(
                "title", "router.authorization",
                "icon", "vi-eos-icons:role-binding",
                "alwaysShow", true
        ));

        List<Map<String, Object>> authChildren = new ArrayList<>();
        authChildren.add(Map.of(
                "path", "department",
                "component", "views/Authorization/Department/Department",
                "name", "Department",
                "meta", Map.of("title", "router.department")
        ));
        if (lvl <= 2) {
            authChildren.add(Map.of(
                    "path", "admin",
                    "component", "views/Authorization/Admin/Admin",
                    "name", "Admin",
                    "meta", Map.of("title", "router.admin")
            ));
        }
        authChildren.add(Map.of(
                "path", "audit",
                "component", "views/Authorization/Audit/Audit",
                "name", "Audit",
                "meta", Map.of("title", "router.audit")
        ));
        authChildren.add(Map.of(
                "path", "user",
                "component", "views/Authorization/User/User",
                "name", "User",
                "meta", Map.of("title", "router.user")
        ));
        authorization.put("children", authChildren);
        routes.add(authorization);

        Map<String, Object> nutrition = new HashMap<>();
        nutrition.put("path", "/nutrition");
        nutrition.put("component", "#");
        nutrition.put("redirect", "/nutrition/brochure");
        nutrition.put("name", "Nutrition");
        nutrition.put("meta", Map.of(
                "title", "router.nutritionBrochure",
                "icon", "vi-ep:food",
                "alwaysShow", true
        ));
        nutrition.put("children", List.of(
                Map.of(
                        "path", "brochure",
                        "component", "views/Nutrition/Brochure",
                        "name", "NutritionBrochure",
                        "meta", Map.of("title", "router.nutritionBrochure")
                )
        ));
        routes.add(nutrition);

        Map<String, Object> userCenter = new HashMap<>();
        userCenter.put("path", "/user-center");
        userCenter.put("component", "#");
        userCenter.put("redirect", "/user-center/feedback");
        userCenter.put("name", "UserCenter");
        userCenter.put("meta", Map.of(
                "title", "router.userCenter",
                "icon", "vi-ep:user",
                "alwaysShow", true
        ));
        userCenter.put("children", List.of(
                Map.of(
                        "path", "feedback",
                        "component", "views/UserCenter/Feedback",
                        "name", "UserCenterFeedback",
                        "meta", Map.of("title", "router.complaintSuggestion")
                )
        ));
        routes.add(userCenter);

        writeAudit(admin, req, "ROLE_LIST", "ROLE", "", "success", Map.of(
                "roleLevel", lvl,
                "bizLine", safeStr(admin.getBizLine()),
                "routeCount", routes.size()
        ));
        return ApiResp.ok(routes);
    }

    @GetMapping("/role/paths")
    public ApiResp<List<String>> rolePaths(HttpServletRequest req) {
        AdminUser admin = currentAdmin();
        int lvl = roleLevel(admin);

        List<String> paths = new ArrayList<>();
        paths.add("/dashboard");
        paths.add("/dashboard/analysis");
        paths.add("/dashboard/workplace");
        paths.add("/authorization");
        paths.add("/authorization/department");
        paths.add("/authorization/user");
        paths.add("/authorization/audit");
        if (lvl <= 2) paths.add("/authorization/admin");
        paths.add("/nutrition");
        paths.add("/nutrition/brochure");
        paths.add("/user-center");
        paths.add("/user-center/feedback");

        writeAudit(admin, req, "ROLE_PATHS", "ROLE", "", "success", Map.of(
                "roleLevel", lvl,
                "bizLine", safeStr(admin.getBizLine()),
                "pathCount", paths.size()
        ));
        return ApiResp.ok(paths);
    }

    @GetMapping("/orders/recharge")
    public ApiResp<PageResp<Map<String, Object>>> rechargeOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before
    ) {
        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        String st = safeStr(status);
        String on = safeStr(orderNo);
        LocalDateTime a = parseDateTimeOrNull(after);
        LocalDateTime b = parseDateTimeOrNull(before);

        Page<PaymentOrder> pg = paymentOrderRepo.pageAdmin(userId, st, on, a, b, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("userId", o.getUserId());
            m.put("packageId", o.getPackageId());
            m.put("payChannel", o.getPayChannel());
            m.put("amountCny", o.getAmountCny());
            m.put("kcoins", o.getKcoins());
            m.put("status", o.getStatus());
            m.put("prepayId", o.getPrepayId());
            m.put("paidAt", o.getPaidAt());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).toList();

        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    @GetMapping("/orders/vip")
    public ApiResp<PageResp<Map<String, Object>>> vipOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before
    ) {
        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        String st = safeStr(status);
        String on = safeStr(orderNo);
        LocalDateTime a = parseDateTimeOrNull(after);
        LocalDateTime b = parseDateTimeOrNull(before);

        Page<VipOrder> pg = vipOrderRepo.pageAdmin(userId, st, on, a, b, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("userId", o.getUserId());
            m.put("planId", o.getPlanId());
            m.put("months", o.getMonths());
            m.put("familySeats", o.getFamilySeats());
            m.put("autoRenew", o.getAutoRenew());
            m.put("amountCny", o.getAmountCny());
            m.put("status", o.getStatus());
            m.put("paidAt", o.getPaidAt());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).toList();

        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    @GetMapping("/orders/spend")
    public ApiResp<PageResp<Map<String, Object>>> spendOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before
    ) {
        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        String st = safeStr(status);
        String on = safeStr(orderNo);
        String bt = safeStr(bizType);
        LocalDateTime a = parseDateTimeOrNull(after);
        LocalDateTime b = parseDateTimeOrNull(before);

        Page<SpendOrder> pg = spendOrderRepo.pageAdmin(userId, st, on, bt, a, b, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("userId", o.getUserId());
            m.put("amountKc", o.getAmountKc());
            m.put("bizType", o.getBizType());
            m.put("bizRef", o.getBizRef());
            m.put("idempotentKey", o.getIdempotentKey());
            m.put("status", o.getStatus());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).toList();

        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    @GetMapping("/orders/refund")
    public ApiResp<PageResp<Map<String, Object>>> refundOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String refundNo,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before
    ) {
        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        String st = safeStr(status);
        String rn = safeStr(refundNo);
        LocalDateTime a = parseDateTimeOrNull(after);
        LocalDateTime b = parseDateTimeOrNull(before);

        Page<RefundOrder> pg = refundOrderRepo.pageAdmin(userId, st, rn, a, b, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("refundNo", o.getRefundNo());
            m.put("userId", o.getUserId());
            m.put("spendOrderId", o.getSpendOrderId());
            m.put("amountKc", o.getAmountKc());
            m.put("idempotentKey", o.getIdempotentKey());
            m.put("status", o.getStatus());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).toList();

        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    @GetMapping("/wallet/tx")
    public ApiResp<PageResp<Map<String, Object>>> walletTx(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before
    ) {
        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        String tp = safeStr(type);
        LocalDateTime a = parseDateTimeOrNull(after);
        LocalDateTime b = parseDateTimeOrNull(before);

        Page<WalletTransaction> pg = walletTransactionRepo.pageAdmin(userId, tp, a, b, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("userId", t.getUserId());
            m.put("type", t.getType());
            m.put("changeKc", t.getChangeKc());
            m.put("balanceAfter", t.getBalanceAfter());
            m.put("refOrderId", t.getRefOrderId());
            m.put("memo", t.getMemo());
            m.put("createdAt", t.getCreatedAt());
            return m;
        }).toList();

        return ApiResp.ok(new PageResp<>(pg.getTotalElements(), list));
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String safeStr(String v) {
        return String.valueOf(v == null ? "" : v).trim();
    }

    private static String safeStr(Object v) {
        return String.valueOf(v == null ? "" : v).trim();
    }

    private static int parseIntOrZero(Object v) {
        try {
            String s = safeStr(v);
            if (s.isEmpty()) return 0;
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static double safeDouble(Object v) {
        try {
            String s = safeStr(v);
            if (s.isEmpty()) return 0.0;
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private static List<Map<String, Object>> toIngredientMaps(List<RecipeIngredient> list) {
        if (list == null || list.isEmpty()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (RecipeIngredient it : list) {
            if (it == null) continue;
            String name = safeStr(it.getName());
            String amount = safeStr(it.getAmount());
            if (name.isEmpty() && amount.isEmpty()) continue;
            out.add(Map.of("name", name, "amount", amount));
        }
        return out;
    }

    private static List<Map<String, Object>> toStepMaps(List<RecipeStep> list) {
        if (list == null || list.isEmpty()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (RecipeStep it : list) {
            if (it == null) continue;
            String title = safeStr(it.getTitle());
            String description = safeStr(it.getDescription());
            int timeMinutes = it.getTimeMinutes() == null ? 0 : Math.max(0, it.getTimeMinutes());
            if (title.isEmpty() && description.isEmpty() && timeMinutes <= 0) continue;
            out.add(Map.of("title", title, "description", description, "timeMinutes", timeMinutes));
        }
        return out;
    }

    private List<RecipeIngredient> parseRecipeIngredients(Object obj) {
        if (obj == null) return List.of();
        Object v = obj;
        if (v instanceof String s) {
            String raw = safeStr(s);
            if (raw.isEmpty()) return List.of();
            try {
                v = objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
                return List.of();
            }
        }
        if (!(v instanceof List<?> list) || list.isEmpty()) return List.of();

        List<RecipeIngredient> out = new ArrayList<>();
        for (Object it : list) {
            if (it instanceof Map<?, ?> m) {
                String name = safeStr(m.get("name"));
                String amount = safeStr(m.get("amount"));
                if (name.isEmpty() && amount.isEmpty()) continue;
                out.add(new RecipeIngredient(name, amount));
            } else {
                String s = safeStr(it);
                if (s.isEmpty()) continue;
                out.add(new RecipeIngredient(s, ""));
            }
        }
        return out;
    }

    private List<RecipeStep> parseRecipeSteps(Object obj) {
        if (obj == null) return List.of();
        Object v = obj;
        if (v instanceof String s) {
            String raw = safeStr(s);
            if (raw.isEmpty()) return List.of();
            try {
                v = objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
                return List.of();
            }
        }
        if (!(v instanceof List<?> list) || list.isEmpty()) return List.of();

        List<RecipeStep> out = new ArrayList<>();
        for (Object it : list) {
            if (it instanceof Map<?, ?> m) {
                String title = safeStr(m.get("title"));
                String description = safeStr(m.get("description"));
                Integer timeMinutes = parseIntOrNull(m.get("timeMinutes"));
                out.add(new RecipeStep(title, description, timeMinutes == null ? 0 : Math.max(0, timeMinutes)));
            } else {
                String s = safeStr(it);
                if (s.isEmpty()) continue;
                out.add(new RecipeStep("", s, 0));
            }
        }
        return out;
    }

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String formatDateTime(LocalDateTime t) {
        if (t == null) return "";
        try {
            return DT_FMT.format(t);
        } catch (Exception ignored) {
            return String.valueOf(t);
        }
    }

    private static Long parseLongOrNull(Object v) {
        try {
            String s = safeStr(v);
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer parseIntOrNull(Object v) {
        try {
            String s = safeStr(v);
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean looksLikeEmail(String s) {
        String v = safeStr(s);
        return !v.isEmpty() && v.contains("@");
    }

    private static boolean looksLikePhone(String s) {
        String v = safeStr(s);
        if (v.isEmpty()) return false;
        int digits = 0;
        for (int i = 0; i < v.length(); i++) {
            if (Character.isDigit(v.charAt(i))) digits++;
        }
        return digits >= 6;
    }

    private void applyAccount(User u, String account, String email) {
        String acc = safeStr(account);
        String em = safeStr(email);

        if (!em.isEmpty()) {
            u.setEmail(em);
        } else if (looksLikeEmail(acc)) {
            u.setEmail(acc);
        }

        if (looksLikePhone(acc)) {
            u.setPhone(acc);
        }
    }

    private String genUniqueUid(String account) {
        String base = safeStr(account);
        if (base.isEmpty() || looksLikeEmail(base) || looksLikePhone(base)) {
            base = "u_" + UUID.randomUUID().toString().replace("-", "");
        }

        String uid = base;
        int i = 0;
        while (userRepo.findByUid(uid).isPresent()) {
            i++;
            uid = base + "_" + i;
        }
        return uid;
    }

    private static String normalizeRole(Object roleObj) {
        if (roleObj == null) return "";
        if (roleObj instanceof String s) return safeStr(s).toUpperCase();
        if (roleObj instanceof List<?> list) {
            for (Object it : list) {
                String v = safeStr(it);
                if ("1".equals(v) || "ADMIN".equalsIgnoreCase(v)) return "ADMIN";
            }
            return "USER";
        }
        return safeStr(roleObj).toUpperCase();
    }

    private static int roleLevel(AdminUser admin) {
        if (admin == null) return 3;
        Integer vv = admin.getRoleLevel();
        if (vv == null) {
            String role = safeStr(admin.getRole()).trim().toUpperCase();
            if ("ADMIN".equals(role)) {
                String biz = normalizeBizLine(admin.getBizLine());
                return biz.isEmpty() ? 1 : 2;
            }
            return 3;
        }
        int v = vv;
        if (v < 1) return 3;
        if (v > 3) return 3;
        return v;
    }

    private static String normalizeBizLine(String v) {
        String s = safeStr(v);
        if (s.isEmpty() || "all".equalsIgnoreCase(s)) return "";
        return s;
    }

    private static String resolveBizLineForList(AdminUser admin, int lvl, String requestBizLine) {
        String reqBiz = normalizeBizLine(requestBizLine);
        if (lvl == 1) return reqBiz;
        String myBiz = safeStr(admin == null ? "" : admin.getBizLine());
        if (myBiz.isEmpty()) {
            if (lvl == 2) return reqBiz;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "未配置业务线");
        }
        return myBiz;
    }

    private static Long resolveCreatorForList(AdminUser admin, int lvl) {
        if (lvl >= 3) return admin == null ? null : admin.getId();
        return null;
    }

    private boolean hasPendingBusiness(Long userId) {
        if (userId == null) return false;
        if (paymentOrderRepo.existsByUserIdAndStatus(userId, "pending")) return true;
        if (vipOrderRepo.existsByUserIdAndStatus(userId, "pending")) return true;
        if (spendOrderRepo.existsByUserIdAndStatus(userId, "pending")) return true;
        return refundOrderRepo.existsByUserIdAndStatus(userId, "pending");
    }

    private boolean hasPendingBusinessForAdmin(Long adminId) {
        if (adminId == null) return false;
        if (paymentOrderRepo.existsPendingByCreatedByAdminId(adminId)) return true;
        if (vipOrderRepo.existsPendingByCreatedByAdminId(adminId)) return true;
        if (spendOrderRepo.existsPendingByCreatedByAdminId(adminId)) return true;
        return refundOrderRepo.existsPendingByCreatedByAdminId(adminId);
    }

    private void assertCanManageAdmin(AdminUser operator, AdminUser target) {
        int lvl = roleLevel(operator);
        int tl = roleLevel(target);
        if (tl <= 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可操作一级管理员");
        }
        if (lvl == 1) return;
        if (lvl == 2) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
    }

    private void assertCanManageRecipeBiz(AdminUser admin, HttpServletRequest req, String action, String resourceId) {
        int lvl = roleLevel(admin);
        if (lvl >= 3) {
            writeAudit(admin, req, action, "RECIPE", safeStr(resourceId), "failed", Map.of("reason", "forbidden"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
        }
        if (lvl == 2) {
            String biz = normalizeBizLine(admin == null ? "" : admin.getBizLine());
            if (!biz.isEmpty() && !"recipe".equalsIgnoreCase(biz)) {
                writeAudit(admin, req, action, "RECIPE", safeStr(resourceId), "failed", Map.of("reason", "bizLine_forbidden", "bizLine", biz));
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");
            }
        }
    }

    private String genUniqueAdminUid() {
        String base = "a_" + UUID.randomUUID().toString().replace("-", "");
        String uid = base;
        int i = 0;
        while (adminUserRepo.findByUid(uid).isPresent()) {
            i++;
            uid = base + "_" + i;
        }
        return uid;
    }

    private static boolean isStrongPassword(String pwd) {
        String s = safeStr(pwd);
        if (s.length() < 12) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') hasUpper = true;
            else if (c >= 'a' && c <= 'z') hasLower = true;
            else if (c >= '0' && c <= '9') hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private static String genStrongPassword() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return "Aa1!" + raw.substring(0, 12);
    }

    private static String extractDepartmentId(Map<String, Object> body) {
        if (body == null) return "";
        Object dept = body.get("department");
        if (dept instanceof String || dept instanceof Number) {
            String v = safeStr(dept);
            if (!v.isEmpty()) return v;
        }
        if (dept instanceof Map<?, ?> m) {
            Object id = m.get("id");
            String v = safeStr(id);
            if (!v.isEmpty()) return v;
            Object value = m.get("value");
            v = safeStr(value);
            if (!v.isEmpty()) return v;
            Object key = m.get("key");
            return safeStr(key);
        }
        Object id = body.get("department.id");
        String v = safeStr(id);
        if (!v.isEmpty()) return v;
        return safeStr(body.get("departmentId"));
    }

    private void writeAudit(AdminUser admin, HttpServletRequest req, String action, String targetType, String targetId, String result, Object detailObj) {
        try {
            AdminAuditLog l = new AdminAuditLog();
            if (admin != null) {
                l.setOperatorAdminId(admin.getId());
                l.setOperatorAdminUid(safeStr(admin.getUid()));
                l.setOperatorRoleLevel(admin.getRoleLevel());
                l.setOperatorBizLine(safeStr(admin.getBizLine()));
            }
            l.setAction(safeStr(action));
            l.setTargetType(safeStr(targetType));
            l.setTargetId(safeStr(targetId));
            l.setIp(clientIp(req));
            l.setResult(safeStr(result));
            l.setDetail(String.valueOf(detailObj == null ? "" : detailObj));
            l.setCreatedAt(LocalDateTime.now());
            adminAuditLogRepo.save(l);
        } catch (Exception ignored) {
        }
    }

    private static String clientIp(HttpServletRequest req) {
        if (req == null) return "";
        String xff = safeStr(req.getHeader("X-Forwarded-For"));
        if (!xff.isEmpty()) {
            int idx = xff.indexOf(',');
            return idx >= 0 ? xff.substring(0, idx).trim() : xff;
        }
        String xrip = safeStr(req.getHeader("X-Real-IP"));
        if (!xrip.isEmpty()) return xrip;
        return safeStr(req.getRemoteAddr());
    }

    private static LocalDateTime parseDateTimeOrNull(String s) {
        try {
            String v = String.valueOf(s == null ? "" : s).trim();
            if (v.isEmpty()) return null;
            return LocalDateTime.parse(v);
        } catch (Exception ignored) {
            return null;
        }
    }
}
