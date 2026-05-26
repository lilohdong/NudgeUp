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

        if (!assigneeId.equals(userId)) {
            teamMemberRepository.findByTeamIdAndUserId(teamId, assigneeId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User assignee = assigneeId.equals(userId)
                ? creator
                : userRepository.findById(assigneeId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Task task = taskRepository.save(Task.builder()
                .teamId(teamId)
                .title(request.getTitle())
                .description(request.getDescription())
                .assigneeId(assigneeId)
                .createdById(userId)
                .dueDate(request.getDueDate())
                .build());

        return TaskResponse.from(task, UserResponse.from(assignee), UserResponse.from(creator), 0);
    }

    public List<TaskResponse> getTeamTasks(Long userId, Long teamId, TaskStatus status) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        List<Task> tasks = status != null
                ? taskRepository.findByTeamIdAndStatus(teamId, status)
                : taskRepository.findByTeamId(teamId);

        return tasks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getMyTasks(Long userId, Long teamId, TaskStatus status) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        List<Task> tasks = status != null
                ? taskRepository.findByTeamIdAndAssigneeIdAndStatus(teamId, userId, status)
                : taskRepository.findByTeamIdAndAssigneeId(teamId, userId);

        return tasks.stream()
                .map(this::toResponse)
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

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        boolean isAssignee = userId.equals(task.getAssigneeId());
        boolean isLeader = member.getRole() == TeamRole.LEADER;
        if (!isAssignee && !isLeader) {
            throw new CustomException(ErrorCode.NO_TASK_PERMISSION);
        }

        TaskStatus newStatus = request.getStatus();
        if (task.getStatus() == TaskStatus.IN_PROGRESS && newStatus == TaskStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        task.changeStatus(newStatus);
        return toResponse(task);
    }

    private TaskResponse toResponse(Task task) {
        User assignee = task.getAssigneeId() != null
                ? userRepository.findById(task.getAssigneeId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                : null;
        User creator = userRepository.findById(task.getCreatedById())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        long nudgeCount = nudgeLogRepository.countByTaskId(task.getId());
        return TaskResponse.from(task,
                assignee != null ? UserResponse.from(assignee) : null,
                UserResponse.from(creator),
                nudgeCount);
    }
}
