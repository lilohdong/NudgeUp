package com.nudgeup.backend.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// TODO: POST /auth/register 요청 바디
// - username: 1~50자, 공백 불가
// - email: 이메일 형식 검증
// - password: 8자 이상
@Getter
public class RegisterRequest {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50)
    private String username;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;
}
