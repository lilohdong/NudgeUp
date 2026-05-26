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

import java.util.Collections;
import java.util.List;
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

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
        }

        TeamMember sender = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        if (sender.getRole() != TeamRole.LEADER && !sender.isCanNudge()) {
            throw new CustomException(ErrorCode.NO_NUDGE_PERMISSION);
        }

        User senderUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User assigneeUser = userRepository.findById(task.getAssigneeId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        fcmService.sendNudgeMessage(FcmMessageDto.builder()
                .token(assigneeUser.getFcmToken())
                .title("독촉 알림")
                .body(senderUser.getUsername() + "님이 '" + task.getTitle() + "' 작업을 독촉했습니다.")
                .data(Collections.singletonMap("taskId", task.getId().toString()))
                .build());

        NudgeLog nudgeLog = nudgeLogRepository.save(NudgeLog.builder()
                .taskId(task.getId())
                .sentById(userId)
                .build());

        return NudgeResponse.builder()
                .nudgeLogId(nudgeLog.getId())
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .sentTo(UserResponse.from(assigneeUser))
                .sentBy(UserResponse.from(senderUser))
                .sentAt(nudgeLog.getSentAt() != null ? nudgeLog.getSentAt().toString() : null)
                .build();
    }

    public List<NudgeLogResponse> getNudgeLogs(Long userId, Long teamId, Long taskId) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        List<NudgeLog> logs;
        if (taskId != null) {
            logs = nudgeLogRepository.findByTaskId(taskId);
        } else {
            List<Long> taskIds = taskRepository.findByTeamId(teamId).stream()
                    .map(Task::getId)
                    .collect(Collectors.toList());
            logs = taskIds.isEmpty() ? List.of() : nudgeLogRepository.findByTaskIdIn(taskIds);
        }

        return logs.stream()
                .map(log -> {
                    Task t = taskRepository.findById(log.getTaskId())
                            .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
                    User sentByUser = userRepository.findById(log.getSentById())
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    User sentToUser = t.getAssigneeId() != null
                            ? userRepository.findById(t.getAssigneeId())
                                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                            : null;
                    return NudgeLogResponse.from(log, t.getTitle(),
                            sentToUser != null ? UserResponse.from(sentToUser) : null,
                            UserResponse.from(sentByUser));
                })
                .collect(Collectors.toList());
    }
}
