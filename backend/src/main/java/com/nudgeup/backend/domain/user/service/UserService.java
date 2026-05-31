package com.nudgeup.backend.domain.user.service;

import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.task.repository.TaskRepository;
import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import com.nudgeup.backend.domain.team.repository.TeamMemberRepository;
import com.nudgeup.backend.domain.team.repository.TeamRepository;
import com.nudgeup.backend.domain.user.dto.request.FcmTokenRequest;
import com.nudgeup.backend.domain.user.dto.request.UpdateColorRequest;
import com.nudgeup.backend.domain.user.dto.request.UpdateUserRequest;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;
    private final NudgeLogRepository nudgeLogRepository;

    public UserResponse getMyProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserResponse updateMyUsername(Long userId, UpdateUserRequest request) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (request.getUsername() != null) {
                        user.updateUsername(request.getUsername());
                    }
                    return UserResponse.from(user);
                })
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserResponse updateMyColor(Long userId, UpdateColorRequest request) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.updateColor(request.getColor());
                    return UserResponse.from(user);
                })
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateFcmToken(Long userId, FcmTokenRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .updateFcmToken(request.getFcmToken());
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<TeamMember> myMemberships = teamMemberRepository.findAllByUserId(userId);
        for (TeamMember m : myMemberships) {
            if (m.getRole() == TeamRole.LEADER) {
                long cnt = teamMemberRepository.countByTeamId(m.getTeamId());
                if (cnt > 1) {
                    throw new CustomException(ErrorCode.LEADER_HAS_MEMBERS);
                }
            }
        }

        // 혼자인 팀 해산: Task → NudgeLog → TeamMember → Team 순으로 삭제
        for (TeamMember m : myMemberships) {
            if (m.getRole() == TeamRole.LEADER) {
                Long teamId = m.getTeamId();
                List<Long> taskIds = taskRepository.findByTeamId(teamId).stream()
                        .map(t -> t.getId()).collect(java.util.stream.Collectors.toList());
                if (!taskIds.isEmpty()) {
                    nudgeLogRepository.deleteByTaskIdIn(taskIds);
                }
                taskRepository.deleteAllByTeamId(teamId);
                teamMemberRepository.deleteAllByTeamId(teamId);
                teamRepository.deleteById(teamId);
            }
        }
        // 남은 비-팀장 멤버십 삭제
        teamMemberRepository.deleteAllByUserId(userId);

        taskRepository.clearAssigneeByUserId(userId);
        nudgeLogRepository.deleteBySentById(userId);
        userRepository.deleteById(userId);
    }
}
