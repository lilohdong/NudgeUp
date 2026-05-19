package com.nudgeup.backend.domain.user.controller;

import com.nudgeup.backend.domain.user.dto.request.FcmTokenRequest;
import com.nudgeup.backend.domain.user.dto.request.UpdateUserRequest;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import com.nudgeup.backend.domain.user.service.UserService;
import com.nudgeup.backend.global.response.ApiResponse;
import com.nudgeup.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// TODO: 사용자 프로필 관련 엔드포인트
// - @AuthenticationPrincipal CustomUserDetails로 현재 로그인 유저 ID 추출
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/v1/users/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // TODO: userService.getMyProfile(userDetails.getUser().getId()) 호출
        return null;
    }

    // PATCH /api/v1/users/me
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateUserRequest request) {
        // TODO: 구현
        return null;
    }

    // PUT /api/v1/users/me/fcm-token
    @PutMapping("/me/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenRequest request) {
        // TODO: 구현
        return null;
    }
}
