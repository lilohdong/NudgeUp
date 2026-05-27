# Task 도메인 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Task 엔티티·Service·Controller의 TODO 스텁을 완전히 구현하여 작업 생성·조회·상태 변경 API를 동작시킨다.

**Architecture:** 기존 team 도메인 패턴을 그대로 따른다 — Service에서 팀 소속·권한 검증 후 Repository 조회, 엔티티 내부 메서드로 상태 변경, 정적 팩토리 `from()`으로 DTO 변환. `NudgeLogRepository.countByTaskId()`로 nudgeCount를 집계해 TaskResponse에 포함한다.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Jakarta Validation, Lombok

---

## 사전 준비

```bash
git checkout main && git pull origin main
git checkout -b feat/task
```

## 수정 파일 목록

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java` | `createdAt` 필드 추가, `@Builder` 생성자 구현, `changeStatus()` 완성 |
| `backend/src/main/java/com/nudgeup/backend/domain/task/dto/response/TaskResponse.java` | `from()` 구현 (dDay 계산 포함) |
| `backend/src/main/java/com/nudgeup/backend/domain/task/service/TaskService.java` | 4개 메서드 전부 구현, `NudgeLogRepository` 의존성 추가 |
| `backend/src/main/java/com/nudgeup/backend/domain/task/controller/TaskController.java` | 4개 엔드포인트 구현 |

---

### Task 1: Task 엔티티 완성

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java`

- [ ] **Step 1: Task.java 전체를 아래 내용으로 교체**

```java
package com.nudgeup.backend.domain.task.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Task(Long teamId, String title, String description,
                Long assigneeId, Long createdById, LocalDate dueDate) {
        this.teamId = teamId;
        this.title = title;
        this.description = description;
        this.assigneeId = assigneeId;
        this.createdById = createdById;
        this.dueDate = dueDate;
        this.status = TaskStatus.PENDING;
    }

    public void changeStatus(TaskStatus newStatus) {
        if (newStatus == TaskStatus.IN_PROGRESS) {
            this.startedAt = LocalDateTime.now();
        } else if (newStatus == TaskStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
        this.status = newStatus;
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java
git commit -m "feat: Task 엔티티 구현 (createdAt, Builder, changeStatus)"
```

---

### Task 2: TaskResponse DTO from() 구현

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/dto/response/TaskResponse.java`

- [ ] **Step 1: TaskResponse.java 전체를 아래 내용으로 교체**

```java
package com.nudgeup.backend.domain.task.dto.response;

import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class TaskResponse {

    private Long taskId;
    private String title;
    private String description;
    private TaskStatus status;
    private UserResponse assignee;
    private UserResponse createdBy;
    private LocalDate dueDate;
    private Long dDay;
    private Long nudgeCount;
    private String startedAt;
    private String completedAt;
    private String createdAt;

    public static TaskResponse from(Task task, UserResponse assignee, UserResponse createdBy, long nudgeCount) {
        Long dDay = task.getDueDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDate())
                : null;
        return TaskResponse.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignee(assignee)
                .createdBy(createdBy)
                .dueDate(task.getDueDate())
                .dDay(dDay)
                .nudgeCount(nudgeCount)
                .startedAt(task.getStartedAt() != null ? task.getStartedAt().toString() : null)
                .completedAt(task.getCompletedAt() != null ? task.getCompletedAt().toString() : null)
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null)
                .build();
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/dto/response/TaskResponse.java
git commit -m "feat: TaskResponse from() 구현 (dDay 계산 포함)"
```

---

### Task 3: TaskService 구현

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/service/TaskService.java`

- [ ] **Step 1: TaskService.java 전체를 아래 내용으로 교체**

```java
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

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User creator = userRepository.findById(userId)
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
                .map(task -> toResponse(task))
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getMyTasks(Long userId, Long teamId, TaskStatus status) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));

        List<Task> tasks = status != null
                ? taskRepository.findByTeamIdAndAssigneeIdAndStatus(teamId, userId, status)
                : taskRepository.findByTeamIdAndAssigneeId(teamId, userId);

        return tasks.stream()
                .map(task -> toResponse(task))
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

        task.changeStatus(request.getStatus());
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
```

- [ ] **Step 2: 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/service/TaskService.java
git commit -m "feat: TaskService 구현 (생성/조회/상태변경)"
```

---

### Task 4: TaskController 구현

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/controller/TaskController.java`

- [ ] **Step 1: TaskController.java 전체를 아래 내용으로 교체**

```java
package com.nudgeup.backend.domain.task.controller;

import com.nudgeup.backend.domain.task.dto.request.CreateTaskRequest;
import com.nudgeup.backend.domain.task.dto.request.UpdateTaskStatusRequest;
import com.nudgeup.backend.domain.task.dto.response.TaskResponse;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.task.service.TaskService;
import com.nudgeup.backend.global.response.ApiResponse;
import com.nudgeup.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody CreateTaskRequest request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("작업이 생성되었습니다.", taskService.createTask(userId, teamId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTeamTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestParam(required = false) TaskStatus status) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("팀 작업 목록 조회 성공", taskService.getTeamTasks(userId, teamId, status)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getMyTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestParam(required = false) TaskStatus status) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("내 작업 목록 조회 성공", taskService.getMyTasks(userId, teamId, status)));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("작업 상태가 변경되었습니다.",
                taskService.updateTaskStatus(userId, teamId, taskId, request)));
    }
}
```

- [ ] **Step 2: 전체 빌드 확인**

```bash
cd backend && ./gradlew compileJava
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋 및 푸시**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/controller/TaskController.java
git commit -m "feat: TaskController 구현 (작업 생성/조회/상태변경 엔드포인트)"
git push origin feat/task
```
