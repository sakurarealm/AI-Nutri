package com.xs.ai.aiheathagent;

import com.xs.ai.aiheathagent.entity.User;
import com.xs.ai.aiheathagent.repo.UserRepo;
import com.xs.ai.aiheathagent.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminAccessTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepo userRepo;
    @Autowired JwtUtil jwtUtil;

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : createdUserIds) {
            try {
                userRepo.deleteById(id);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void adminCanAccessAdminEndpoints() throws Exception {
        String uid = "t_admin_" + UUID.randomUUID();
        User u = new User();
        u.setUid(uid);
        u.setRole("ADMIN");
        u.setStatus(1);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        User saved = userRepo.save(u);
        createdUserIds.add(saved.getId());

        String token = jwtUtil.createAccessToken(uid);

        mockMvc.perform(get("/api/admin/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.uid").value(uid));
    }

    @Test
    void normalUserCannotAccessAdminEndpoints() throws Exception {
        String uid = "t_user_" + UUID.randomUUID();
        User u = new User();
        u.setUid(uid);
        u.setRole("USER");
        u.setStatus(1);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        User saved = userRepo.save(u);
        createdUserIds.add(saved.getId());

        String token = jwtUtil.createAccessToken(uid);

        mockMvc.perform(get("/api/admin/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}

