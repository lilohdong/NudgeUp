package com.nudgeup.backend.domain.team.dto.response;

import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

// TODO: 팀원 정보 응답 DTO (팀 상세 조회 시 members 배열의 각 항목)
// - from(TeamMember): 엔티티 → DTO 변환
@Getter
@Builder
public class TeamMemberResponse {

    private Long userId;
    private String username;
    private String color;
    private TeamRole role;
    private boolean canNudge;
    private String joinedAt;

    public static TeamMemberResponse from(TeamMember member) {
        // TODO: 구현
        return null;
    }
}
