package com.nudgeup.backend.domain.team.dto.request;

import lombok.Getter;

import java.util.List;

// TODO: PATCH /teams/{teamId}/settings 요청 바디 (팀장 전용)
// - teamName, newLeaderId, memberPermissions 모두 Optional (null이면 해당 항목 변경 안 함)
// - MemberPermission: userId + canNudge 쌍
@Getter
public class UpdateTeamSettingsRequest {

    private String teamName;
    private Long newLeaderId;
    private List<MemberPermission> memberPermissions;

    @Getter
    public static class MemberPermission {
        private Long userId;
        private boolean canNudge;
    }
}
