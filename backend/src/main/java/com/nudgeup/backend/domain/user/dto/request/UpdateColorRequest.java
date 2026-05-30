package com.nudgeup.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class UpdateColorRequest {

    @NotNull(message = "색상은 필수입니다.")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상은 HEX 형식이어야 합니다. (예: #A78BFA)")
    private String color;
}
