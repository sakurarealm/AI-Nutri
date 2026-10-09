package com.xs.ai.aiheathagent.admin.service;

import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.dto.resp.PageResp;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.PaymentOrderRepo;
import com.xs.ai.aiheathagent.repo.RefundOrderRepo;
import com.xs.ai.aiheathagent.repo.SpendOrderRepo;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.repo.VipOrderRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminUserService {
    private final AdminOperatorService adminOperatorService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepo userRepo;
    private final PaymentOrderRepo paymentOrderRepo;
    private final VipOrderRepo vipOrderRepo;
    private final SpendOrderRepo spendOrderRepo;
    private final RefundOrderRepo refundOrderRepo;

    public AdminUserService(
            AdminOperatorService adminOperatorService,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            UserRepo userRepo,
            PaymentOrderRepo paymentOrderRepo,
            VipOrderRepo vipOrderRepo,
            SpendOrderRepo spendOrderRepo,
            RefundOrderRepo refundOrderRepo
    ) {
        this.adminOperatorService = adminOperatorService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.userRepo = userRepo;
        this.paymentOrderRepo = paymentOrderRepo;
        this.vipOrderRepo = vipOrderRepo;
        this.spendOrderRepo = spendOrderRepo;
        this.refundOrderRepo = refundOrderRepo;
    }

    public AdminUser currentAdmin() {
        return adminOperatorService.currentAdmin();
    }

    public int roleLevel(AdminUser admin) {
        return AdminOperatorService.roleLevel(admin);
    }

    public PageResp<Map<String, Object>> listUsers(int page, int pageSize, boolean onlyActive, String username, String account, String id) {
        AdminUser admin = currentAdmin();
        int lvl = roleLevel(admin);
        String bizLine = resolveBizLineForList(admin, lvl, safeStr(id));
        Long createdByAdminId = resolveCreatorForList(admin, lvl);
        boolean onlyManaged = lvl >= 3;

        int p = Math.max(page - 1, 0);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(p, size);

        Page<User> pg = userRepo.pageAdmin(
                safeStr(username),
                safeStr(account),
                onlyActive,
                onlyManaged,
                bizLine,
                createdByAdminId,
                pageable
        );

        List<Map<String, Object>> list = pg.getContent().stream().map(u -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", String.valueOf(u.getId()));
            m.put("username", safeStr(u.getNickname()));
            String acc = safeStr(u.getPhone());
            if (acc.isEmpty()) acc = safeStr(u.getUid());
            if (acc.isEmpty()) acc = safeStr(u.getEmail());
            m.put("account", acc);
            m.put("email", safeStr(u.getEmail()));
            m.put("createTime", u.getCreatedAt() == null ? "" : String.valueOf(u.getCreatedAt()));
            m.put("role", safeStr(u.getRole()));
            m.put("status", u.getStatus() == null ? 1 : u.getStatus());
            Long creator = u.getCreatedByAdminId();
            m.put("createdByAdminId", creator == null ? null : String.valueOf(creator));
            boolean managed;
            if (lvl >= 3) {
                managed = creator != null && admin.getId() != null && admin.getId().equals(creator);
            } else if (lvl == 2) {
                String myBiz = safeStr(admin.getBizLine());
                String ub = safeStr(u.getBizLine());
                managed = myBiz.isEmpty() || ub.isEmpty() || myBiz.equals(ub);
            } else {
                managed = true;
            }
            m.put("managed", managed);
            String deptId = safeStr(u.getBizLine());
            java.util.Map<String, Object> dept = new java.util.HashMap<>();
            dept.put("id", deptId.isEmpty() ? "all" : deptId);
            dept.put("departmentName", deptId.isEmpty() ? "全部" : deptId);
            m.put("department", dept);
            return m;
        }).toList();

        return new PageResp<>(pg.getTotalElements(), list);
    }

    public void resetUserPassword(Map<String, Object> body) {
        AdminUser operator = currentAdmin();
        int lvl = roleLevel(operator);
        if (lvl >= 3) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限");

        if (body == null) body = Map.of();
        Long id = parseLongOrNull(body.get("id"));
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少用户ID");

        String pwd = safeStr(body.get("password"));
        if (!isStrongPassword(pwd)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码需≥12位且包含大小写字母、数字、特殊字符");

        User target = userRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (lvl == 2) {
            String myBiz = safeStr(operator.getBizLine());
            String ub = safeStr(target.getBizLine());
            if (!myBiz.isEmpty() && !ub.isEmpty() && !myBiz.equals(ub)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可修改非本业务线成员");
            }
        }
        if (passwordEncoder == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "密码服务未初始化");
        target.setPasswordHash(passwordEncoder.encode(pwd));
        target.setUpdatedAt(LocalDateTime.now());
        userRepo.save(target);
    }

    @Transactional
    public void hardDeleteUsers(Map<String, Object> body) {
        AdminUser admin = currentAdmin();
        int lvl = roleLevel(admin);
        if (lvl >= 3) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无删除权限");

        Object idsObj = body == null ? null : body.get("ids");
        if (!(idsObj instanceof List<?> ids) || ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要删除的数据");

        List<Long> userIds = new ArrayList<>();
        for (Object it : ids) {
            Long v = parseLongOrNull(it);
            if (v != null) userIds.add(v);
        }
        if (userIds.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要删除的数据");

        List<User> users = userRepo.findAllById(userIds);
        if (lvl == 2) {
            String myBiz = safeStr(admin.getBizLine());
            if (!myBiz.isEmpty()) {
                for (User u : users) {
                    String ub = safeStr(u.getBizLine());
                    if (!ub.isEmpty() && !myBiz.equals(ub)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可删除非本业务线成员");
                }
            }
        }

        for (User u : users) {
            if (hasPendingBusiness(u.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "存在未完成业务，禁止删除");
        }

        cascadeDeleteByUserIdColumns(userIds);
        userRepo.deleteAllByIdInBatch(userIds);
    }

    public User saveUser(Map<String, Object> body) {
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
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需绑定业务线");
                    }
                } else {
                    if (bizLineFromBody.isEmpty() || "all".equalsIgnoreCase(bizLineFromBody)) bizLineFromBody = myBiz;
                    if (!myBiz.equals(bizLineFromBody)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可创建非本业务线成员");
                }
            } else if (lvl >= 3) {
                String myBiz = safeStr(admin.getBizLine());
                if (myBiz.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "未配置业务线");
                if (bizLineFromBody.isEmpty() || "all".equalsIgnoreCase(bizLineFromBody)) bizLineFromBody = myBiz;
                if (!myBiz.equals(bizLineFromBody)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可创建非本业务线成员");
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
            if (passwordEncoder == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "密码服务未初始化");
            u.setPasswordHash(passwordEncoder.encode("123456"));
            return userRepo.save(u);
        }

        User u = userRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (lvl == 2) {
            String myBiz = safeStr(admin.getBizLine());
            if (!myBiz.isEmpty()) {
                String ub = safeStr(u.getBizLine());
                if (!ub.isEmpty() && !myBiz.equals(ub)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可修改非本业务线成员");
            }
        } else if (lvl >= 3) {
            if (u.getCreatedByAdminId() == null || admin.getId() == null || !admin.getId().equals(u.getCreatedByAdminId())) {
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
                    if (!myBiz.isEmpty() && !myBiz.equals(bizLineFromBody)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不可变更为非本业务线");
                }
                u.setBizLine(normalizeBizLine(bizLineFromBody));
            }
        }
        u.setUpdatedAt(now);
        return userRepo.save(u);
    }

    private boolean hasPendingBusiness(Long userId) {
        if (userId == null) return false;
        if (paymentOrderRepo.existsByUserIdAndStatus(userId, "pending")) return true;
        if (vipOrderRepo.existsByUserIdAndStatus(userId, "pending")) return true;
        if (spendOrderRepo.existsByUserIdAndStatus(userId, "pending")) return true;
        return refundOrderRepo.existsByUserIdAndStatus(userId, "pending");
    }

    private void cascadeDeleteByUserIdColumns(List<Long> userIds) {
        List<Long> ids = userIds == null ? List.of() : userIds.stream().filter(x -> x != null && x > 0).distinct().toList();
        if (ids.isEmpty()) return;

        List<Map<String, Object>> cols = List.of();
        try {
            cols = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME AS t, COLUMN_NAME AS c " +
                            "FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME IN ('user_id', 'userId')"
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "级联删除初始化失败");
        }

        if (cols != null) {
            for (Map<String, Object> row : cols) {
                String table = safeStr(row.get("t"));
                String col = safeStr(row.get("c"));
                if (table.isEmpty() || col.isEmpty()) continue;
                String tLower = table.toLowerCase();
                if ("users".equals(tLower) || "conversation_records".equals(tLower)) continue;
                if (!isSafeSqlIdent(table) || !isSafeSqlIdent(col)) continue;
                execDeleteIn(table, col, ids);
            }
        }

        if (isSafeSqlIdent("conversation_records") && isSafeSqlIdent("user_id")) {
            execDeleteIn("conversation_records", "user_id", ids);
        }
        if (isSafeSqlIdent("conversation_records") && isSafeSqlIdent("userId")) {
            execDeleteIn("conversation_records", "userId", ids);
        }
    }

    private void execDeleteIn(String table, String column, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM `").append(table).append("` WHERE `").append(column).append("` IN (");
        for (int i = 0; i < userIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        sb.append(")");
        try {
            jdbcTemplate.update(sb.toString(), userIds.toArray());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "删除失败，无法清理关联数据");
        }
    }

    private static boolean isSafeSqlIdent(String v) {
        String s = safeStr(v);
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) return false;
        }
        return true;
    }

    private static String safeStr(String v) {
        return String.valueOf(v == null ? "" : v).trim();
    }

    private static String safeStr(Object v) {
        return String.valueOf(v == null ? "" : v).trim();
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
}
