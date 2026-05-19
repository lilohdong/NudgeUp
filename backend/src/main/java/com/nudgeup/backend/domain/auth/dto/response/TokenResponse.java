package com.nudgeup.backend.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// TODO: 토큰 갱신(/auth/refresh) 응답 DTO
@Getter
@Builder
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
}
