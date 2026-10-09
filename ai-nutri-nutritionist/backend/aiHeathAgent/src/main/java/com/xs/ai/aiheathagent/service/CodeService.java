package com.xs.ai.aiheathagent.service;



import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.xs.ai.aiheathagent.entity.VerificationCode;
import com.xs.ai.aiheathagent.repo.VerificationCodeRepo;
import com.xs.ai.aiheathagent.util.AccountUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Random;


@Service
public class CodeService {
    private final VerificationCodeRepo repo;
    private final MailService mailService;
    private final Random random = new Random();
    public CodeService(VerificationCodeRepo repo, MailService mailService){ this.repo = repo; this.mailService = mailService; }


    @Transactional
    public String send(String account, String scene, String ip){
        String code = String.format("%06d", random.nextInt(1000000));
        VerificationCode v = new VerificationCode();
        v.setScene(scene); v.setTarget(account); v.setCode(code);
        v.setExpireAt(LocalDateTime.now().plusMinutes(10));
        v.setAttemptCount(0); v.setMaxAttempts(5);
        v.setIp(ip); v.setCreatedAt(LocalDateTime.now());
        repo.save(v);
        if (AccountUtil.isEmail(account)) {
            mailService.sendVerificationCode(account, code, displayNameForScene(scene));
        }
// 这里对接短信/邮件供应商；当前返回固定成功，并将验证码直接返回给前端用于开发联调
        return code;
    }

    private static String displayNameForScene(String scene) {
        String s = scene == null ? "" : scene.toLowerCase();
        if (s.contains("admin")) return "管理员";
        if (s.contains("register")) return "新用户";
        return "用户";
    }


    public VerificationCode validateOrThrow(String account, String scene, String code){
        VerificationCode v = repo.findTopByTargetAndSceneAndUsedAtIsNullOrderByIdDesc(account, scene)
                .orElseThrow(() -> new BizEx(ErrorCode.BIZ_ERROR, "请先获取验证码"));
        if (v.getExpireAt().isBefore(LocalDateTime.now())) throw new BizEx(ErrorCode.BIZ_ERROR, "验证码已过期");
        if (!v.getCode().equals(code)) throw new BizEx(ErrorCode.BIZ_ERROR, "验证码错误");
        if (v.getAttemptCount()!=null && v.getMaxAttempts()!=null && v.getAttemptCount() >= v.getMaxAttempts())
            throw new BizEx(ErrorCode.TOO_MANY_REQUESTS, "验证码错误次数过多，请重新获取");
        return v;
    }
    @Transactional
    public void markUsed(String account, String scene){
        repo.findTopByTargetAndSceneAndUsedAtIsNullOrderByIdDesc(account, scene)
                .ifPresent(v -> { v.setUsedAt(LocalDateTime.now()); repo.save(v); });
    }

}
