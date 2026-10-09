package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class MailService {
    private final JavaMailSender mailSender;
    private final Environment env;

    @Value("${app.mail.from-name:}")
    private String fromName;

    @Value("${app.mail.template-path:邮箱模板.txt}")
    private String templatePath;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider, Environment env) {
        this.mailSender = mailSenderProvider.getIfAvailable(JavaMailSenderImpl::new);
        this.env = env;
    }

    public void sendVerificationCode(String toEmail, String code, String displayName) {
        String fromEmail = resolveFromEmail();
        if (fromEmail.isBlank()) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "邮件服务未配置（spring.mail.username 为空；请确认后端启动进程已注入 SMTP_USER/SPRING_MAIL_USERNAME，Windows 下需重启启动该进程的终端/IDE）");
        }
        ensureMailSenderCredentials(fromEmail);
        EmailTemplate tpl = loadTemplate();
        String subject = render(tpl.subject, displayName, code);
        String body = render(tpl.body, displayName, code);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            if (fromName != null && !fromName.isBlank()) helper.setFrom(fromEmail, fromName);
            else helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (Exception e) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "邮件发送失败");
        }
    }

    private String resolveFromEmail() {
        String v = safeTrim(env.getProperty("spring.mail.username"));
        if (!v.isEmpty()) return v;
        v = safeTrim(env.getProperty("SPRING_MAIL_USERNAME"));
        if (!v.isEmpty()) return v;
        v = safeTrim(env.getProperty("SMTP_USER"));
        if (!v.isEmpty()) return v;
        if (mailSender instanceof JavaMailSenderImpl impl) {
            v = safeTrim(impl.getUsername());
            if (!v.isEmpty()) return v;
        }
        v = safeTrim(System.getenv("SPRING_MAIL_USERNAME"));
        if (!v.isEmpty()) return v;
        return safeTrim(System.getenv("SMTP_USER"));
    }

    private void ensureMailSenderCredentials(String fromEmail) {
        if (!(mailSender instanceof JavaMailSenderImpl impl)) return;
        if (safeTrim(impl.getUsername()).isEmpty()) {
            impl.setUsername(fromEmail);
        }
        String pwd = safeTrim(env.getProperty("spring.mail.password"));
        if (pwd.isEmpty()) pwd = safeTrim(env.getProperty("SPRING_MAIL_PASSWORD"));
        if (pwd.isEmpty()) pwd = safeTrim(env.getProperty("SMTP_PASS"));
        if (safeTrim(impl.getPassword()).isEmpty() && !pwd.isEmpty()) {
            impl.setPassword(pwd);
        }
    }

    private static String safeTrim(Object v) {
        return String.valueOf(v == null ? "" : v).trim();
    }

    private EmailTemplate loadTemplate() {
        String subject = "验证码";
        String body = "尊敬的 {员工姓名}：\n您的验证码为：{6 位验证码}\n请在 10 分钟内完成验证。";
        try {
            Path path = Path.of(templatePath);
            if (!path.isAbsolute()) {
                Path base = Path.of(System.getProperty("user.dir"));
                Path candidate = base.resolve(templatePath);
                if (Files.exists(candidate)) {
                    path = candidate;
                } else if (base.getParent() != null) {
                    Path candidate2 = base.getParent().resolve(templatePath);
                    if (Files.exists(candidate2)) path = candidate2;
                    else path = candidate;
                } else {
                    path = candidate;
                }
            }
            if (!Files.exists(path)) return new EmailTemplate(subject, body);
            List<String> raw = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) {
                String s = raw.get(i);
                if (i == 0 && s != null && s.startsWith("\uFEFF")) s = s.substring(1);
                lines.add(s == null ? "" : s.replace("\r", ""));
            }
            int titleIdx = indexOfLine(lines, "标题");
            if (titleIdx >= 0) {
                int j = titleIdx + 1;
                while (j < lines.size() && lines.get(j).trim().isEmpty()) j++;
                if (j < lines.size()) subject = lines.get(j).trim();
            }
            int bodyIdx = indexOfLine(lines, "正文");
            if (bodyIdx >= 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = bodyIdx + 1; i < lines.size(); i++) {
                    sb.append(lines.get(i));
                    if (i < lines.size() - 1) sb.append("\n");
                }
                String rendered = sb.toString();
                if (!rendered.isEmpty()) body = rendered;
            }
            return new EmailTemplate(subject, body);
        } catch (Exception e) {
            return new EmailTemplate(subject, body);
        }
    }

    private static int indexOfLine(List<String> lines, String exact) {
        for (int i = 0; i < lines.size(); i++) {
            if (exact.equals(lines.get(i).trim())) return i;
        }
        return -1;
    }

    private static String render(String raw, String displayName, String code) {
        String name = (displayName == null || displayName.isBlank()) ? "用户" : displayName.trim();
        String out = raw == null ? "" : raw;
        out = out.replace("{员工姓名}", name);
        out = out.replace("{6 位验证码}", code == null ? "" : code);
        out = out.replace("{6位验证码}", code == null ? "" : code);
        return out;
    }

    private record EmailTemplate(String subject, String body) {
    }
}
