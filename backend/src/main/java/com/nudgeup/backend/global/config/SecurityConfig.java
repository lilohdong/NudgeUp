package com.nudgeup.backend.global.config;

import com.nudgeup.backend.global.security.jwt.JwtFilter;
import com.nudgeup.backend.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// TODO: Spring Security 전체 설정
// - CSRF 비활성화 (REST API, 세션 미사용)
// - SessionCreationPolicy.STATELESS 설정
// - 허용 경로: POST /api/v1/auth/register, POST /api/v1/auth/login, POST /api/v1/auth/refresh
// - 그 외 모든 요청 인증 필요
// - JwtFilter를 UsernamePasswordAuthenticationFilter 앞에 삽입
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // TODO: 구현
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
