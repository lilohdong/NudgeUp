package com.nudgeup.backend.domain.team.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

// TODO: PATCH /teams/{teamId}/settings 요청 바디 (팀장 전용)
// - teamName, newLeaderId, memberPermissions 모두 Optional (null이면 해당 항목 변경 안 함)
// - MemberPermission: userId + canNudge 쌍
@Getter
public class UpdateTeamSettingsRequest {

    // null이면 변경 안 함. 값이 있으면 1~100자 범위만 허용 (빈 문자열 방지)
    @Size(min = 1, max = 100, message = "팀 이름은 1~100자여야 합니다.")
    private String teamName;
    private Long newLeaderId;
    private List<MemberPermission> memberPermissions;

    @Getter
    public static class MemberPermission {
        private Long userId;
        private boolean canNudge;
    }
}
