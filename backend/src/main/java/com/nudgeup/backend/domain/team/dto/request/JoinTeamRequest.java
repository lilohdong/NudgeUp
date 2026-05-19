package com.nudgeup.backend.domain.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

// TODO: POST /teams/join 요청 바디
@Getter
public class JoinTeamRequest {

    @NotBlank(message = "초대 코드는 필수입니다.")
    private String inviteCode;
}
