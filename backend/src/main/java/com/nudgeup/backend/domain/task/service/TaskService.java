package com.nudgeup.backend.domain.task.service;

import com.nudgeup.backend.domain.task.dto.request.CreateTaskRequest;
import com.nudgeup.backend.domain.task.dto.request.UpdateTaskStatusRequest;
import com.nudgeup.backend.domain.task.dto.response.TaskResponse;
import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.task.repository.TaskRepository;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import com.nudgeup.backend.domain.team.repository.TeamMemberRepository;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO: 작업 생성/조회/상태 변경 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(Long userId, Long teamId, CreateTaskRequest request) {
        // TODO: 구현
        // 1. 팀 소속 검증
        // 2. assigneeId가 null이면 userId로 지정
        // 3. assigneeId가 있으면 해당 유저가 팀 소속인지 검증
        // 4. Task.builder()...build() → taskRepository.save()
        // 5. TaskResponse.from() 반환
        return null;
    }

    public List<TaskResponse> getTeamTasks(Long userId, Long teamId, TaskStatus status) {
        // TODO: 구현
        // 1. 팀 소속 검증
        // 2. status != null ? findByTeamIdAndStatus() : findByTeamId()
        // 3. 각 Task에 대해 assignee, createdBy User 조회 후 TaskResponse.from() 매핑
        return null;
    }

    public List<TaskResponse> getMyTasks(Long userId, Long teamId, TaskStatus status) {
        // TODO: 구현 (getTeamTasks와 동일하나 assigneeId = userId 고정)
        return null;
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long userId, Long teamId, Long taskId,
                                         UpdateTaskStatusRequest request) {
        // TODO: 구현
        // 1. Task 조회 → TASK_NOT_FOUND
        // 2. 해당 팀 작업인지 확인 (task.getTeamId() == teamId)
        // 3. 권한 검증: 담당자(assigneeId == userId) 또는 팀장(LEADER)만 가능 → NO_TASK_PERMISSION
        // 4. task.changeStatus() 호출
        // 5. TaskResponse.from() 반환
        return null;
    }
}
