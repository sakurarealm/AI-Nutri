package com.xs.ai.aiheathagent.config;


import com.xs.ai.aiheathagent.admin.entity.AdminUser;
import com.xs.ai.aiheathagent.admin.repo.AdminUserRepo;
import com.xs.ai.aiheathagent.entity.RechargePackage;
import com.xs.ai.aiheathagent.repo.RechargePackageRepo;
import com.xs.ai.aiheathagent.util.Ids;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.math.BigDecimal;
import java.time.LocalDateTime;


@Configuration @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final RechargePackageRepo repo;
    private final AdminUserRepo adminUserRepo;
    private final PasswordEncoder passwordEncoder;
    @Override public void run(String... args){
        ensureAdminUser();
        if (repo.count() > 0) return;
        repo.save(pkg("60 K币", 6, 60, 0, 0, 1));
        repo.save(pkg("100 K币",10, 100,5, 0, 2));
        repo.save(pkg("320 K币",30, 320,30,1, 3));
        repo.save(pkg("550 K币",50, 550,70,0, 4));
        repo.save(pkg("1150 K币",100,1150,200,1, 5));
        repo.save(pkg("2400 K币",200,2400,500,0, 6));
    }

    private void ensureAdminUser() {
        final String defaultPwd = "admin";
        var existed = adminUserRepo.findByUsername("admin").orElse(null);
        if (existed != null) {
            String hash = String.valueOf(existed.getPasswordHash() == null ? "" : existed.getPasswordHash()).trim();
            boolean shouldResetPassword = hash.isEmpty()
                    || passwordEncoder.matches("Admin@123456", hash)
                    || passwordEncoder.matches("Admin@123", hash);
            if (shouldResetPassword && !passwordEncoder.matches(defaultPwd, hash)) {
                existed.setPasswordHash(passwordEncoder.encode(defaultPwd));
            }
            if (String.valueOf(existed.getRole() == null ? "" : existed.getRole()).trim().isEmpty()) {
                existed.setRole("ADMIN");
            }
            if (existed.getRoleLevel() == null) existed.setRoleLevel(1);
            if (existed.getStatus() == null || existed.getStatus() == 0) existed.setStatus(1);
            existed.setFailedLoginCount(0);
            existed.setLockedUntil(null);
            if (String.valueOf(existed.getNickname() == null ? "" : existed.getNickname()).trim().isEmpty()) {
                existed.setNickname("系统管理员");
            }
            if (existed.getUpdatedAt() == null) existed.setUpdatedAt(LocalDateTime.now());
            adminUserRepo.save(existed);
            return;
        }

        AdminUser u = new AdminUser();
        u.setUid(Ids.uid());
        u.setUsername("admin");
        u.setNickname("系统管理员");
        u.setRole("ADMIN");
        u.setRoleLevel(1);
        u.setStatus(1);
        u.setPasswordHash(passwordEncoder.encode(defaultPwd));
        LocalDateTime now = LocalDateTime.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        adminUserRepo.save(u);
    }

    private RechargePackage pkg(String name, int price, int kcoins, int gift, int recommend, int sort){
        RechargePackage p = new RechargePackage();
        p.setName(name); p.setPriceCny(new BigDecimal(price)); p.setKcoins(kcoins); p.setGift(gift);
        p.setIsRecommend(recommend); p.setStatus(1); p.setSortNo(sort); return p;
    }
}
