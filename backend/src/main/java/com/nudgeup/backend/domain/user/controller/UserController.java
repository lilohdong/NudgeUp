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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", userService.getMyProfile(userId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUserRequest request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", userService.updateMyProfile(userId, request)));
    }

    @PutMapping("/me/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenRequest request) {
        Long userId = userDetails.getUser().getId();
        userService.updateFcmToken(userId, request);
        return ResponseEntity.ok(ApiResponse.success("FCM 토큰이 등록되었습니다.", null));
    }
}
