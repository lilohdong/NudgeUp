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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final NudgeLogRepository nudgeLogRepository;

    // ── 헬퍼: 팀 멤버십 검증 ────────────────────────────────────────────────
    private TeamMember getTeamMemberOrThrow(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
    }

    // ── 헬퍼: task 목록 → 배치 조회 후 TaskResponse 목록 반환 ──────────────
    private List<TaskResponse> buildTaskResponses(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }

        // taskIds 수집
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());

        // nudge count 배치 조회 → Map<taskId, count>
        Map<Long, Long> nudgeCountMap = nudgeLogRepository.countByTaskIdIn(taskIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // assigneeId + createdById 수집
        Set<Long> userIds = new HashSet<>();
        for (Task task : tasks) {
            if (task.getAssigneeId() != null) userIds.add(task.getAssigneeId());
            if (task.getCreatedById() != null) userIds.add(task.getCreatedById());
        }

        // 사용자 배치 조회 → Map<id, UserResponse>
        Map<Long, UserResponse> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, UserResponse::from));

        return tasks.stream()
                .map(task -> {
                    UserResponse assignee = task.getAssigneeId() != null
                            ? userMap.get(task.getAssigneeId())
                            : null;
                    UserResponse createdBy = task.getCreatedById() != null
                            ? userMap.get(task.getCreatedById())
                            : null;
                    long nudgeCount = nudgeCountMap.getOrDefault(task.getId(), 0L);
                    return TaskResponse.from(task, assignee, createdBy, nudgeCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse createTask(Long userId, Long teamId, CreateTaskRequest request) {
        getTeamMemberOrThrow(teamId, userId);

        Long assigneeId = request.getAssigneeId() != null ? request.getAssigneeId() : userId;

        if (request.getAssigneeId() != null) {
            getTeamMemberOrThrow(teamId, request.getAssigneeId());
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
        getTeamMemberOrThrow(teamId, userId);

        List<Task> tasks = status != null
                ? taskRepository.findByTeamIdAndStatus(teamId, status)
                : taskRepository.findByTeamIdAndStatusNot(teamId, TaskStatus.CANCELLED);

        return buildTaskResponses(tasks);
    }

    public List<TaskResponse> getMyTasks(Long userId, Long teamId, TaskStatus status) {
        getTeamMemberOrThrow(teamId, userId);

        List<Task> tasks = status != null
                ? taskRepository.findByTeamIdAndAssigneeIdAndStatus(teamId, userId, status)
                : taskRepository.findByTeamIdAndAssigneeIdAndStatusNot(teamId, userId, TaskStatus.CANCELLED);

        return buildTaskResponses(tasks);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long userId, Long teamId, Long taskId,
                                         UpdateTaskStatusRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        if (!task.getTeamId().equals(teamId)) {
            throw new CustomException(ErrorCode.TASK_NOT_FOUND);
        }

        TeamMember member = getTeamMemberOrThrow(teamId, userId);

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
