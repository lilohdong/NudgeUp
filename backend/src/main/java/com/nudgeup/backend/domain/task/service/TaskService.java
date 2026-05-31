package com.nudgeup.backend.domain.task.service;

import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.task.dto.request.CreateTaskRequest;
import com.nudgeup.backend.domain.task.dto.request.UpdateTaskStatusRequest;
import com.nudgeup.backend.domain.task.dto.response.TaskResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final NudgeLogRepository nudgeLogRepository;

    @Transactional
    public TaskResponse createTask(Long userId, Long teamId, CreateTaskRequest request) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        Long assigneeId = request.getAssigneeId() != null ? request.getAssigneeId() : userId;

        if (request.getAssigneeId() != null) {
            teamMemberRepository.findByTeamIdAndUserId(teamId, request.getAssigneeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
        }

        Task task = taskRepository.save(Task.builder()
                .teamId(teamId)
                .title(request.getTitle())
                .description(request.getDescription())
                .assigneeId(assigneeId)
                .createdById(userId)
                .dueDate(request.getDueDate())
                .build());

        UserResponse assignee = userRepository.findById(task.getAssigneeId())
                .map(UserResponse::from)
                .orElse(null);
        UserResponse createdBy = userRepository.findById(task.getCreatedById())
                .map(UserResponse::from)
                .orElse(null);

        return TaskResponse.from(task, assignee, createdBy, 0L);
    }

    public List<TaskResponse> getTeamTasks(Long userId, Long teamId, TaskStatus status) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        List<Task> tasks = status != null
                ? taskRepository.findByTeamIdAndStatus(teamId, status)
                : taskRepository.findByTeamId(teamId);

        return tasks.stream()
                .map(task -> {
                    UserResponse assignee = task.getAssigneeId() != null
                            ? userRepository.findById(task.getAssigneeId()).map(UserResponse::from).orElse(null)
                            : null;
                    UserResponse createdBy = userRepository.findById(task.getCreatedById())
                            .map(UserResponse::from).orElse(null);
                    long nudgeCount = nudgeLogRepository.countByTaskId(task.getId());
                    return TaskResponse.from(task, assignee, createdBy, nudgeCount);
                })
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getMyTasks(Long userId, Long teamId, TaskStatus status) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        List<Task> tasks = status != null
                ? taskRepository.findByTeamIdAndAssigneeIdAndStatus(teamId, userId, status)
                : taskRepository.findByTeamIdAndAssigneeId(teamId, userId);

        return tasks.stream()
                .map(task -> {
                    UserResponse assignee = userRepository.findById(task.getAssigneeId())
                            .map(UserResponse::from).orElse(null);
                    UserResponse createdBy = userRepository.findById(task.getCreatedById())
                            .map(UserResponse::from).orElse(null);
                    long nudgeCount = nudgeLogRepository.countByTaskId(task.getId());
                    return TaskResponse.from(task, assignee, createdBy, nudgeCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long userId, Long teamId, Long taskId,
                                         UpdateTaskStatusRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        if (!task.getTeamId().equals(teamId)) {
            throw new CustomException(ErrorCode.TASK_NOT_FOUND);
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        boolean isAssignee  = userId.equals(task.getAssigneeId());
        boolean isCreatedBy = userId.equals(task.getCreatedById());
        boolean isLeader    = member.getRole() == TeamRole.LEADER;

        boolean isCancelRequest = request.getStatus() == TaskStatus.CANCELLED;
        boolean hasPermission = isAssignee || isLeader || (isCancelRequest && isCreatedBy);

        if (!hasPermission) {
            throw new CustomException(ErrorCode.NO_TASK_PERMISSION);
        }

        task.changeStatus(request.getStatus());

        UserResponse assignee = task.getAssigneeId() != null
                ? userRepository.findById(task.getAssigneeId()).map(UserResponse::from).orElse(null)
                : null;
        UserResponse createdBy = userRepository.findById(task.getCreatedById())
                .map(UserResponse::from).orElse(null);
        long nudgeCount = nudgeLogRepository.countByTaskId(task.getId());

        return TaskResponse.from(task, assignee, createdBy, nudgeCount);
    }
}
