package com.xs.ai.aiheathagent.service;


import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.xs.ai.aiheathagent.dto.resp.MeResp;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;


@Service
public class UserService {
    private final UserRepo userRepo;
    public UserService(UserRepo userRepo){ this.userRepo = userRepo; }


    public User getByUidOrThrow(String uid){
        return userRepo.findByUid(uid).orElseThrow(() -> new BizEx(ErrorCode.UNAUTHORIZED, "未登录"));
    }


    public MeResp toMe(User u){
        return new MeResp(
                u.getUid(), u.getUid(), u.getNickname(), u.getAvatarUrl(), u.getAvatarUrl(),
                u.getGender(), u.getBirthDate()==null? null: u.getBirthDate().toString(),
                u.getHeightCm()==null? null: u.getHeightCm(),
                u.getWeightKg()==null? null: u.getWeightKg()
        );
    }


    @Transactional
    public void updateProfile(User u, String nickname, String avatarUrl, Integer gender, String birthDateStr, BigDecimal height, BigDecimal weight){
        if (nickname!=null) u.setNickname(nickname);
        if (avatarUrl!=null) u.setAvatarUrl(avatarUrl);
        if (gender!=null) u.setGender(gender);
        if (birthDateStr!=null && !birthDateStr.isBlank()) u.setBirthDate(LocalDate.parse(birthDateStr));
        if (height!=null) u.setHeightCm(height);
        if (weight!=null) u.setWeightKg(weight);
        u.setUpdatedAt(LocalDateTime.now());
        userRepo.save(u);
    }
}