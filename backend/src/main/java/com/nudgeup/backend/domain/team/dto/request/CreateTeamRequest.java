package com.nudgeup.backend.domain.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// TODO: POST /teams 요청 바디
@Getter
public class CreateTeamRequest {

    @NotBlank(message = "팀 이름은 필수입니다.")
    @Size(max = 100, message = "팀 이름은 100자 이하여야 합니다.")
    private String teamName;
}
