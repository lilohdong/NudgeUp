package com.nudgeup.backend.domain.auth.controller;

import com.nudgeup.backend.domain.auth.dto.request.LoginRequest;
import com.nudgeup.backend.domain.auth.dto.request.RegisterRequest;
import com.nudgeup.backend.domain.auth.dto.request.TokenRefreshRequest;
import com.nudgeup.backend.domain.auth.dto.response.LoginResponse;
import com.nudgeup.backend.domain.auth.dto.response.TokenResponse;
import com.nudgeup.backend.domain.auth.service.AuthService;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import com.nudgeup.backend.global.response.ApiResponse;
import com.nudgeup.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// TODO: 인증 관련 엔드포인트 (회원가입, 로그인, 토큰 갱신, 로그아웃)
// - /auth/** 경로는 SecurityConfig에서 permitAll() 설정 (logout 제외)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        // TODO: authService.register() 호출, 201 반환
        return null;
    }

    // POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        // TODO: authService.login() 호출, 200 반환
        return null;
    }

    // POST /api/v1/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        // TODO: authService.refresh() 호출, 200 반환
        return null;
    }

    // POST /api/v1/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // TODO: authService.logout() 호출, 200 반환
        return null;
    }
}
