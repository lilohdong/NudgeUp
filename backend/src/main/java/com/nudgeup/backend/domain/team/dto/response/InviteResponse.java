package com.nudgeup.backend.domain.team.dto.response;

import lombok.Builder;
import lombok.Getter;

// TODO: GET /teams/{teamId}/invite 응답 DTO
// - inviteLink 형식: "nudgeup://join?code={inviteCode}" (딥링크 스킴)
@Getter
@Builder
public class InviteResponse {

    private String inviteCode;
    private String inviteLink;
}
