package com.nudgeup.backend.domain.team.service;

import com.nudgeup.backend.domain.team.dto.request.CreateTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.JoinTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.UpdateTeamSettingsRequest;
import com.nudgeup.backend.domain.team.dto.response.*;
import com.nudgeup.backend.domain.team.entity.Team;
import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.task.repository.TaskRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final NudgeLogRepository nudgeLogRepository;

    // Fix 4: private helper to eliminate duplicated orElseThrow pattern
    private TeamMember getTeamMemberOrThrow(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
    }

    @Transactional
    public TeamDetailResponse createTeam(Long userId, CreateTeamRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String inviteCode = generateUniqueInviteCode();
        Team team = teamRepository.save(Team.builder()
                .teamName(request.getTeamName())
                .leaderId(userId)
                .inviteCode(inviteCode)
                .color(request.getColor())
                .build());
        TeamMember member = teamMemberRepository.save(TeamMember.builder()
                .teamId(team.getId())
                .user(user)
                .role(TeamRole.LEADER)
                .canNudge(true)
                .build());
        return TeamDetailResponse.from(team, List.of(TeamMemberResponse.from(member)));
    }

    // Fix 3: N+1 eliminated — fetch all teams and counts in bulk
    public List<TeamResponse> getMyTeams(Long userId) {
        List<TeamMember> memberships = teamMemberRepository.findAllByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> teamIds = memberships.stream()
                .map(TeamMember::getTeamId)
                .collect(Collectors.toList());

        Map<Long, Team> teamMap = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, t -> t));

        Map<Long, Long> countMap = teamMemberRepository.countByTeamIdIn(teamIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return memberships.stream()
                .map(membership -> {
                    Team team = teamMap.get(membership.getTeamId());
                    if (team == null) {
                        throw new CustomException(ErrorCode.TEAM_NOT_FOUND);
                    }
                    int memberCount = countMap.getOrDefault(team.getId(), 0L).intValue();
                    return TeamResponse.builder()
                            .teamId(team.getId())
                            .teamName(team.getTeamName())
                            .leaderId(team.getLeaderId())
                            .memberCount(memberCount)
                            .myRole(membership.getRole())
                            .createdAt(team.getCreatedAt().toString())
                            .color(team.getColor())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public TeamDetailResponse getTeamDetail(Long userId, Long teamId) {
        getTeamMemberOrThrow(teamId, userId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));
        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(teamId).stream()
                .map(TeamMemberResponse::from)
                .collect(Collectors.toList());
        return TeamDetailResponse.from(team, members);
    }

    public InviteResponse getInviteCode(Long userId, Long teamId) {
        getTeamMemberOrThrow(teamId, userId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));
        return InviteResponse.builder()
                .inviteCode(team.getInviteCode())
                .inviteLink("nudgeup://join?code=" + team.getInviteCode())
                .build();
    }

    @Transactional
    public TeamResponse joinTeam(Long userId, JoinTeamRequest request) {
        Team team = teamRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_CODE));
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId)) {
            throw new CustomException(ErrorCode.ALREADY_TEAM_MEMBER);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        teamMemberRepository.save(TeamMember.builder()
                .teamId(team.getId())
                .user(user)
                .role(TeamRole.MEMBER)
                .canNudge(false)
                .build());
        int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());
        return TeamResponse.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .leaderId(team.getLeaderId())
                .memberCount(memberCount)
                .myRole(TeamRole.MEMBER)
                .createdAt(team.getCreatedAt().toString())
                .color(team.getColor())
                .build();
    }

    @Transactional
    public void updateTeamSettings(Long userId, Long teamId, UpdateTeamSettingsRequest request) {
        TeamMember myMembership = getTeamMemberOrThrow(teamId, userId);
        if (myMembership.getRole() != TeamRole.LEADER) {
            throw new CustomException(ErrorCode.NOT_TEAM_LEADER);
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));
        if (request.getTeamName() != null) {
            team.updateTeamName(request.getTeamName());
        }
        if (request.getNewLeaderId() != null) {
            myMembership.updateRole(TeamRole.MEMBER);
            TeamMember newLeader = getTeamMemberOrThrow(teamId, request.getNewLeaderId());
            newLeader.updateRole(TeamRole.LEADER);
            team.updateLeader(request.getNewLeaderId());
        }
        if (request.getMemberPermissions() != null) {
            for (UpdateTeamSettingsRequest.MemberPermission permission : request.getMemberPermissions()) {
                teamMemberRepository.findByTeamIdAndUserId(teamId, permission.getUserId())
                        .ifPresent(m -> m.updateNudgePermission(permission.isCanNudge()));
            }
        }
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (teamRepository.existsByInviteCode(code));
        return code;
    }

    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        TeamMember member = getTeamMemberOrThrow(teamId, userId);
        if (member.getRole() == TeamRole.LEADER) {
            long memberCount = teamMemberRepository.countByTeamId(teamId);
            if (memberCount > 1) {
                throw new CustomException(ErrorCode.LEADER_CANNOT_LEAVE);
            }
            dissolveTeam(teamId);
            return;
        }
        teamMemberRepository.delete(member);
    }

    void dissolveTeam(Long teamId) {
        List<Long> taskIds = taskRepository.findByTeamId(teamId).stream()
                .map(task -> task.getId())
                .collect(Collectors.toList());
        if (!taskIds.isEmpty()) {
            nudgeLogRepository.deleteByTaskIdIn(taskIds);
        }
        taskRepository.deleteAllByTeamId(teamId);
        teamMemberRepository.deleteAllByTeamId(teamId);
        teamRepository.deleteById(teamId);
    }

    @Transactional
    public void kickMember(Long leaderId, Long teamId, Long targetUserId) {
        TeamMember leader = getTeamMemberOrThrow(teamId, leaderId);
        if (leader.getRole() != TeamRole.LEADER) {
            throw new CustomException(ErrorCode.NOT_TEAM_LEADER);
        }
        if (leaderId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_KICK_SELF);
        }
        TeamMember target = getTeamMemberOrThrow(teamId, targetUserId);
        teamMemberRepository.delete(target);
    }
}
