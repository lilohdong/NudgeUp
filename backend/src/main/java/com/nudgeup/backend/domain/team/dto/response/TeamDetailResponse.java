package com.nudgeup.backend.domain.team.dto.response;

import com.nudgeup.backend.domain.team.entity.Team;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamDetailResponse {

    private Long teamId;
    private String teamName;
    private Long leaderId;
    private String inviteCode;
    private String inviteLink;
    private String createdAt;
    private List<TeamMemberResponse> members;

    public static TeamDetailResponse from(Team team, List<TeamMemberResponse> members) {
        return TeamDetailResponse.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .leaderId(team.getLeaderId())
                .inviteCode(team.getInviteCode())
                .inviteLink("nudgeup://join?code=" + team.getInviteCode())
                .createdAt(team.getCreatedAt().toString())
                .members(members)
                .build();
    }
}
