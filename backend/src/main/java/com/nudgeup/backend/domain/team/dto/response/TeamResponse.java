package com.nudgeup.backend.domain.team.dto.response;

import com.nudgeup.backend.domain.team.entity.Team;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

// TODO: 내 팀 목록 조회 시 각 팀 정보 응답 DTO
// - myRole: 현재 로그인 유저의 해당 팀에서의 역할
// - memberCount: 팀원 수
@Getter
@Builder
public class TeamResponse {

    private Long teamId;
    private String teamName;
    private Long leaderId;
    private int memberCount;
    private TeamRole myRole;
    private String createdAt;
}
