package com.nudgeup.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// TODO: POST /auth/refresh 요청 바디
@Getter
public class TokenRefreshRequest {

    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;
}
