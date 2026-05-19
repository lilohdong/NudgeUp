package com.nudgeup.backend.domain.team.service;

import com.nudgeup.backend.domain.team.dto.request.CreateTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.JoinTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.UpdateTeamSettingsRequest;
import com.nudgeup.backend.domain.team.dto.response.*;
import com.nudgeup.backend.domain.team.entity.Team;
import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import com.nudgeup.backend.domain.team.repository.TeamMemberRepository;
import com.nudgeup.backend.domain.team.repository.TeamRepository;
import com.nudgeup.backend.domain.user.entity.User;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// TODO: 팀 생성/조회/참가/설정/탈퇴 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamDetailResponse createTeam(Long userId, CreateTeamRequest request) {
        // TODO: 구현
        // 1. User 조회
        // 2. inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        // 3. Team.builder()...build() → teamRepository.save()
        // 4. 생성자를 LEADER로 TeamMember 저장 (canNudge=true)
        // 5. TeamDetailResponse 반환
        return null;
    }

    public List<TeamResponse> getMyTeams(Long userId) {
        // TODO: 구현
        // 1. teamMemberRepository.findAllByUserId(userId)로 멤버십 목록 조회
        // 2. 각 teamId로 Team 조회 후 TeamResponse 매핑
        return null;
    }

    public TeamDetailResponse getTeamDetail(Long userId, Long teamId) {
        // TODO: 구현
        // 1. 팀 소속 검증: teamMemberRepository.findByTeamIdAndUserId() → 없으면 NOT_TEAM_MEMBER
        // 2. teamMemberRepository.findByTeamId()로 전체 멤버 조회
        // 3. TeamDetailResponse.from() 반환
        return null;
    }

    public InviteResponse getInviteCode(Long userId, Long teamId) {
        // TODO: 팀 소속 검증 후 inviteCode 조회, InviteResponse 반환
        return null;
    }

    @Transactional
    public TeamResponse joinTeam(Long userId, JoinTeamRequest request) {
        // TODO: 구현
        // 1. teamRepository.findByInviteCode() → 없으면 INVALID_INVITE_CODE
        // 2. 중복 참가 검사: existsByTeamIdAndUserId() → ALREADY_TEAM_MEMBER
        // 3. MEMBER 역할로 TeamMember 저장 (canNudge=false)
        return null;
    }

    @Transactional
    public void updateTeamSettings(Long userId, Long teamId, UpdateTeamSettingsRequest request) {
        // TODO: 구현
        // 1. teamMemberRepository.findByTeamIdAndUserId() → 없으면 NOT_TEAM_MEMBER
        // 2. LEADER 여부 검증 → 아니면 NOT_TEAM_LEADER
        // 3. request.getTeamName() != null → team.updateTeamName()
        // 4. request.getNewLeaderId() != null → 기존 팀장 MEMBER로, 새 팀장 LEADER로 + team.updateLeader()
        // 5. memberPermissions 순회 → teamMember.updateNudgePermission()
    }

    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        // TODO: 구현
        // 1. 멤버십 조회 → NOT_TEAM_MEMBER
        // 2. LEADER면 LEADER_CANNOT_LEAVE throw
        // 3. TeamMember 삭제
    }
}
