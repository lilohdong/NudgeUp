package com.nudgeup.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FcmTokenRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String fcmToken;
}
