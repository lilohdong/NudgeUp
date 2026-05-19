package com.nudgeup.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// TODO: PUT /users/me/fcm-token 요청 바디
// - fcmToken: Firebase에서 발급된 디바이스 토큰 (필수값)
@Getter
public class FcmTokenRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String fcmToken;
}
