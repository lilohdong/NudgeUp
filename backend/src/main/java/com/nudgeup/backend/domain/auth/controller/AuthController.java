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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다.", null));
    }
}
