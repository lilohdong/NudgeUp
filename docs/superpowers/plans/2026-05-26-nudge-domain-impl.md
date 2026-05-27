# Nudge 도메인 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** NudgeLog 엔티티·Repository·DTO·Service·Controller의 TODO 스텁을 완성하여 독촉 알림 발송 및 로그 조회 API를 동작시킨다.

**Architecture:** Task 도메인 패턴과 동일. Service에서 권한 검증(LEADER 또는 canNudge=true) 후 FcmService 호출 → NudgeLog 저장 → NudgeResponse 반환. 로그 조회는 taskId 유무에 따라 단건/전체 분기.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Firebase Admin SDK (FCM), Lombok

---

## 사전 준비

```bash
git checkout feat/task
```

(feat/task 브랜치에 이어서 작업)

## 수정 파일 목록

| 파일 | 작업 |
|------|------|
| `backend/.../nudge/entity/NudgeLog.java` | `sentAt` 필드 추가, `@Builder` 생성자 구현 |
| `backend/.../nudge/repository/NudgeLogRepository.java` | `findByTaskIdIn()` JPQL 쿼리 추가 |
| `backend/.../nudge/dto/response/NudgeLogResponse.java` | `from()` 구현 |
| `backend/.../nudge/service/NudgeService.java` | `sendNudge()`, `getNudgeLogs()` 구현 |
| `backend/.../nudge/controller/NudgeController.java` | 2개 엔드포인트 구현 |

---

### Task 1: NudgeLog 엔티티 + Repository + DTO 완성

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/entity/NudgeLog.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/dto/response/NudgeLogResponse.java`

- [ ] **Step 1: NudgeLog.java 전체 교체**

```java
package com.nudgeup.backend.domain.nudge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "nudge_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NudgeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "sent_by_id", nullable = false)
    private Long sentById;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    @Builder
    public NudgeLog(Long taskId, Long sentById) {
        this.taskId = taskId;
        this.sentById = sentById;
    }
}
```

- [ ] **Step 2: NudgeLogRepository.java 전체 교체**

```java
package com.nudgeup.backend.domain.nudge.repository;

import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NudgeLogRepository extends JpaRepository<NudgeLog, Long> {

    List<NudgeLog> findByTaskId(Long taskId);

    long countByTaskId(Long taskId);

    @Query("SELECT n FROM NudgeLog n WHERE n.taskId IN :taskIds ORDER BY n.sentAt DESC")
    List<NudgeLog> findByTaskIdIn(@Param("taskIds") List<Long> taskIds);
}
```

- [ ] **Step 3: NudgeLogResponse.java 전체 교체**

```java
package com.nudgeup.backend.domain.nudge.dto.response;

import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NudgeLogResponse {

    private Long nudgeLogId;
    private Long taskId;
    private String taskTitle;
    private UserResponse sentTo;
    private UserResponse sentBy;
    private String sentAt;

    public static NudgeLogResponse from(NudgeLog log, String taskTitle,
                                        UserResponse sentTo, UserResponse sentBy) {
        return NudgeLogResponse.builder()
                .nudgeLogId(log.getId())
                .taskId(log.getTaskId())
                .taskTitle(taskTitle)
                .sentTo(sentTo)
                .sentBy(sentBy)
                .sentAt(log.getSentAt() != null ? log.getSentAt().toString() : null)
                .build();
    }
}
```

- [ ] **Step 4: 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/entity/NudgeLog.java
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/dto/response/NudgeLogResponse.java
git commit -m "feat: NudgeLog 엔티티, Repository, DTO 구현"
```

---

### Task 2: NudgeService 구현

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/service/NudgeService.java`

- [ ] **Step 1: NudgeService.java 전체 교체**

```java
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
```

- [ ] **Step 2: 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/service/NudgeService.java
git commit -m "feat: NudgeService 구현 (독촉 발송/로그 조회)"
```

---

### Task 3: NudgeController 구현

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/controller/NudgeController.java`

- [ ] **Step 1: NudgeController.java 전체 교체**

```java
package com.nudgeup.backend.domain.nudge.controller;

import com.nudgeup.backend.domain.nudge.dto.response.NudgeLogResponse;
import com.nudgeup.backend.domain.nudge.dto.response.NudgeResponse;
import com.nudgeup.backend.domain.nudge.service.NudgeService;
import com.nudgeup.backend.global.response.ApiResponse;
import com.nudgeup.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}")
@RequiredArgsConstructor
public class NudgeController {

    private final NudgeService nudgeService;

    @PostMapping("/tasks/{taskId}/nudge")
    public ResponseEntity<ApiResponse<NudgeResponse>> sendNudge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("독촉 알림을 발송했습니다.",
                nudgeService.sendNudge(userId, teamId, taskId)));
    }

    @GetMapping("/nudge-logs")
    public ResponseEntity<ApiResponse<List<NudgeLogResponse>>> getNudgeLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestParam(required = false) Long taskId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("독촉 로그 조회 성공",
                nudgeService.getNudgeLogs(userId, teamId, taskId)));
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
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/controller/NudgeController.java
git commit -m "feat: NudgeController 구현 (독촉 발송/로그 조회 엔드포인트)"
git push origin feat/task
```
