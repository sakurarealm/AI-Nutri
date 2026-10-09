package com.xs.ai.aiheathagent.controller;



import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.dto.resp.MeResp;
import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.service.UserService;
import com.xs.ai.aiheathagent.service.VipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.xs.ai.aiheathagent.dto.req.UpdateProfileReq;

import java.util.Map;


@RestController @RequestMapping("/api/user")
public class UserController {
    private final UserRepo userRepo; private final UserService userService; private final VipService vipService;
    public UserController(UserRepo userRepo, UserService userService, VipService vipService){ this.userRepo = userRepo; this.userService = userService; this.vipService = vipService; }


    private User current(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth==null || auth.getPrincipal()==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) {
            if (u.getId() == null || u.getUid() == null || u.getUid().isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
            }
            return u;
        }
        String uid = principal.toString();
        return userRepo.findByUid(uid).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
    }


    @GetMapping("/me")
    public ApiResp<MeResp> me(){
        User u = current();
        return ApiResp.ok(userService.toMe(u));
    }


    @PatchMapping("/me")
    public ApiResp<?> update(@Valid @RequestBody UpdateProfileReq req){
        User u = current();
        userService.updateProfile(u, req.getNickname(), req.getAvatarUrl(), req.getGender(), req.getBirthDate(), req.getHeightCm(), req.getWeightKg());
        return ApiResp.ok(userService.toMe(u));
    }

    @GetMapping("/vip")
    public ApiResp<Map<String, Object>> vip(){
        User u = current();
        return ApiResp.ok(vipService.vipView(u.getId()));
    }
}
