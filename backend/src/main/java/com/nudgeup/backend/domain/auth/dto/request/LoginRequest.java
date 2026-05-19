package com.nudgeup.backend.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// TODO: POST /auth/login 요청 바디
@Getter
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
