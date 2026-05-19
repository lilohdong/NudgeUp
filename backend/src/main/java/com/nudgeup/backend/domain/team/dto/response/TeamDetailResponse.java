package com.nudgeup.backend.domain.team.dto.response;

import com.nudgeup.backend.domain.team.entity.Team;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// TODO: GET /teams/{teamId} 응답 DTO
// - members: TeamMemberResponse 리스트 포함
// - from(Team, List<TeamMemberResponse>): 엔티티 + 멤버 목록 → DTO 변환
@Getter
@Builder
public class TeamDetailResponse {

    private Long teamId;
    private String teamName;
    private Long leaderId;
    private String inviteCode;
    private String createdAt;
    private List<TeamMemberResponse> members;

    public static TeamDetailResponse from(Team team, List<TeamMemberResponse> members) {
        // TODO: 구현
        return null;
    }
}
