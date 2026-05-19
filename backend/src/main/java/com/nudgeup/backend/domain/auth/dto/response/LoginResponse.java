package com.nudgeup.backend.domain.auth.dto.response;

import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

// TODO: POST /auth/login 응답 DTO
// - accessToken, refreshToken, user(UserResponse) 포함
@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private UserResponse user;
}
