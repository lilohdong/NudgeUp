package com.nudgeup.backend.domain.team.dto.response;

import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

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
        return TeamMemberResponse.builder()
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .color(member.getUser().getColor())
                .role(member.getRole())
                .canNudge(member.isCanNudge())
                .joinedAt(member.getJoinedAt().toString())
                .build();
    }
}
