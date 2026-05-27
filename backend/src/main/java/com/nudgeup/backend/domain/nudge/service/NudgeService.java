package com.nudgeup.backend.domain.nudge.service;

import com.nudgeup.backend.domain.nudge.dto.response.NudgeLogResponse;
import com.nudgeup.backend.domain.nudge.dto.response.NudgeResponse;
import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.task.repository.TaskRepository;
import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import com.nudgeup.backend.domain.team.repository.TeamMemberRepository;
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

import java.util.List;
import java.util.Map;
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

    @Transactional
    public NudgeResponse sendNudge(Long userId, Long teamId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        if (!task.getTeamId().equals(teamId)) {
            throw new CustomException(ErrorCode.TASK_NOT_FOUND);
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
        }

        TeamMember sender = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

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
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        List<NudgeLog> logs;
        if (taskId != null) {
            Task targetTask = taskRepository.findById(taskId)
                    .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
            if (!targetTask.getTeamId().equals(teamId)) {
                throw new CustomException(ErrorCode.TASK_NOT_FOUND);
            }
            logs = nudgeLogRepository.findByTaskId(taskId);
        } else {
            List<Long> taskIds = taskRepository.findByTeamId(teamId).stream()
                    .map(Task::getId)
                    .collect(Collectors.toList());
            logs = taskIds.isEmpty() ? List.of() : nudgeLogRepository.findByTaskIdIn(taskIds);
        }

        return logs.stream()
                .map(log -> {
                    Task task = taskRepository.findById(log.getTaskId())
                            .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
                    User sentByUser = userRepository.findById(log.getSentById())
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    UserResponse sentByResponse = UserResponse.from(sentByUser);
                    UserResponse sentToResponse = null;
                    if (task.getAssigneeId() != null) {
                        User assignee = userRepository.findById(task.getAssigneeId())
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                        sentToResponse = UserResponse.from(assignee);
                    }
                    return NudgeLogResponse.from(log, task.getTitle(), sentToResponse, sentByResponse);
                })
                .collect(Collectors.toList());
    }
}
