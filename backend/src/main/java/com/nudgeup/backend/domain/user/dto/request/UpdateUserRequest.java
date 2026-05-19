package com.nudgeup.backend.domain.user.dto.request;

import lombok.Getter;

// TODO: PATCH /users/me 요청 바디
// - username, color 모두 Optional (null이면 변경하지 않음)
// - color는 HEX 형식 검증 정규식 추가: @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
@Getter
public class UpdateUserRequest {

    private String username;
    private String color;
}
