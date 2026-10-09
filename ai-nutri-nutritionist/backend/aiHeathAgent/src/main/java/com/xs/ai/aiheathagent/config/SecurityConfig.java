package com.xs.ai.aiheathagent.config;

import com.xs.ai.aiheathagent.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/auth/**", "/users/register", "/api/auth/**", "/api/api/auth/**").permitAll()
                        .requestMatchers("/admin/auth/**", "/api/admin/auth/**", "/api/api/admin/auth/**").permitAll()
                        .requestMatchers("/api/chat", "/api/api/chat", "/chat", "/api/chat/stream", "/api/api/chat/stream", "/chat/stream").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tts", "/api/api/tts", "/tts").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/stt", "/api/api/stt", "/stt").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/food/recognize", "/api/api/food/recognize", "/food/recognize").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/system/ping", "/api/api/system/ping", "/system/ping", "/api/ping", "/ping").permitAll()
                        .requestMatchers("/api/admin/**", "/api/api/admin/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
