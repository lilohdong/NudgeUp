package com.nudgeup.backend.domain.nudge.service;

import com.nudgeup.backend.domain.nudge.dto.response.NudgeLogResponse;
import com.nudgeup.backend.domain.nudge.dto.response.NudgeResponse;
import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.task.repository.TaskRepository;
import com.nudgeup.backend.domain.team.entity.Team;
import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import com.nudgeup.backend.domain.team.repository.TeamMemberRepository;
import com.nudgeup.backend.domain.team.repository.TeamRepository;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import com.nudgeup.backend.domain.user.entity.User;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import com.nudgeup.backend.global.fcm.FcmMessageDto;
import com.nudgeup.backend.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NudgeService {

    private final NudgeLogRepository nudgeLogRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final TeamRepository teamRepository;

    // ── 헬퍼: 팀 멤버십 검증 ────────────────────────────────────────────────
    private TeamMember getTeamMemberOrThrow(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
    }

    @Transactional
    public NudgeResponse sendNudge(Long userId, Long teamId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        if (!task.getTeamId().equals(teamId)) {
            throw new CustomException(ErrorCode.TASK_NOT_FOUND);
        }

        // 문제 3: CANCELLED 상태도 차단
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
        }

        TeamMember sender = getTeamMemberOrThrow(teamId, userId);

        if (sender.getRole() != TeamRole.LEADER && !sender.isCanNudge()) {
            throw new CustomException(ErrorCode.NO_NUDGE_PERMISSION);
        }

        User senderUser = sender.getUser();
        UserResponse sentBy = UserResponse.from(senderUser);

        UserResponse sentTo = null;
        if (task.getAssigneeId() != null) {
            User receiver = userRepository.findById(task.getAssigneeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            sentTo = UserResponse.from(receiver);

            if (receiver.getFcmToken() != null) {
                FcmMessageDto fcmMessageDto = FcmMessageDto.builder()
                        .token(receiver.getFcmToken())
                        .title("독촉 알림")
                        .body(senderUser.getUsername() + "님이 '" + task.getTitle() + "' 작업을 독촉했습니다.")
                        .data(Map.of("taskId", String.valueOf(taskId), "teamId", String.valueOf(teamId)))
                        .build();
                fcmService.sendNudgeMessage(fcmMessageDto);
            }
        }

        NudgeLog savedLog = nudgeLogRepository.save(
                NudgeLog.builder()
                        .taskId(taskId)
                        .sentById(userId)
                        .build()
        );

        return NudgeResponse.builder()
                .nudgeLogId(savedLog.getId())
                .taskId(taskId)
                .taskTitle(task.getTitle())
                .sentTo(sentTo)
                .sentBy(sentBy)
                .sentAt(savedLog.getSentAt() != null ? savedLog.getSentAt().toString() : null)
                .build();
    }

    public List<NudgeLogResponse> getNudgeLogs(Long userId, Long teamId, Long taskId) {
        getTeamMemberOrThrow(teamId, userId);

        List<NudgeLog> logs;
        if (taskId != null) {
            Task targetTask = taskRepository.findById(taskId)
                    .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
            if (!targetTask.getTeamId().equals(teamId)) {
                throw new CustomException(ErrorCode.TASK_NOT_FOUND);
            }
            logs = nudgeLogRepository.findByTaskId(taskId);
        } else {
            // 문제 4: 엔티티 전체 로딩 대신 ID만 조회
            List<Long> taskIds = taskRepository.findIdsByTeamId(teamId);
            logs = taskIds.isEmpty() ? List.of() : nudgeLogRepository.findByTaskIdIn(taskIds);
        }

        if (logs.isEmpty()) {
            return List.of();
        }

        // 문제 2: N+1 제거 — taskIds, sentByIds 배치 조회
        List<Long> logTaskIds = logs.stream().map(NudgeLog::getTaskId).distinct().collect(Collectors.toList());
        List<Long> sentByIds  = logs.stream().map(NudgeLog::getSentById).distinct().collect(Collectors.toList());

        Map<Long, Task> taskMap = taskRepository.findAllById(logTaskIds)
                .stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        Set<Long> userIds = new HashSet<>(sentByIds);
        taskMap.values().forEach(t -> {
            if (t.getAssigneeId() != null) userIds.add(t.getAssigneeId());
        });

        Map<Long, UserResponse> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, UserResponse::from));

        return logs.stream()
                .map(log -> {
                    Task task = taskMap.get(log.getTaskId());
                    if (task == null) throw new CustomException(ErrorCode.TASK_NOT_FOUND);
                    UserResponse sentByResponse = userMap.get(log.getSentById());
                    UserResponse sentToResponse = task.getAssigneeId() != null
                            ? userMap.get(task.getAssigneeId())
                            : null;
                    return NudgeLogResponse.from(log, task.getTitle(), sentToResponse, sentByResponse);
                })
                .collect(Collectors.toList());
    }

    public List<NudgeLogResponse> getReceivedNudgeLogs(Long userId) {
        List<NudgeLog> logs = nudgeLogRepository.findReceivedByUserId(userId);

        if (logs.isEmpty()) {
            return List.of();
        }

        // 문제 2: N+1 제거 — taskIds, sentByIds, teamIds 배치 조회
        List<Long> logTaskIds = logs.stream().map(NudgeLog::getTaskId).distinct().collect(Collectors.toList());
        List<Long> sentByIds  = logs.stream().map(NudgeLog::getSentById).distinct().collect(Collectors.toList());

        Map<Long, Task> taskMap = taskRepository.findAllById(logTaskIds)
                .stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        List<Long> teamIds = taskMap.values().stream()
                .map(Task::getTeamId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> teamNameMap = teamRepository.findAllById(teamIds)
                .stream()
                .collect(Collectors.toMap(Team::getId, Team::getTeamName));

        Set<Long> userIds = new HashSet<>(sentByIds);
        taskMap.values().forEach(t -> {
            if (t.getAssigneeId() != null) userIds.add(t.getAssigneeId());
        });

        Map<Long, UserResponse> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, UserResponse::from));

        return logs.stream()
                .map(log -> {
                    Task task = taskMap.get(log.getTaskId());
                    if (task == null) throw new CustomException(ErrorCode.TASK_NOT_FOUND);
                    UserResponse sentByResponse = userMap.get(log.getSentById());
                    UserResponse sentToResponse = task.getAssigneeId() != null
                            ? userMap.get(task.getAssigneeId())
                            : null;
                    String teamName = teamNameMap.getOrDefault(task.getTeamId(), "");
                    return NudgeLogResponse.from(
                            log, task.getTitle(),
                            task.getTeamId(), teamName,
                            sentToResponse, sentByResponse);
                })
                .collect(Collectors.toList());
    }

    public long getReceivedNudgeCount(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return nudgeLogRepository.countReceivedByUserIdSince(userId, since);
    }
}
