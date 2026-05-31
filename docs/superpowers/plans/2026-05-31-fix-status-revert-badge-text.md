# fix/status-revert-and-badge-text Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 완료된 작업을 진행중으로 되돌릴 수 있게 하고, 기한 초과 작업의 배지 텍스트를 "기한초과"로 수정한다.

**Architecture:** 백엔드 `Task.changeStatus()` 메서드에서 COMPLETED → IN_PROGRESS 전환 제한을 제거하고, 앱 `TaskListAdapter`에서 dday < 0인 미완료 작업의 배지 텍스트를 교체한다.

**Tech Stack:** Java 17, Spring Boot 3, Android (Java), JUnit 5

---

## File Map

| 파일 | 변경 유형 |
|---|---|
| `backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java` | Modify |
| `backend/src/test/java/com/nudgeup/backend/domain/task/entity/TaskTest.java` | Create |
| `app/app/src/main/java/com/nudgeup/app/ui/main/adapter/TaskListAdapter.java` | Modify |

---

## Task 1: 브랜치 생성

**Files:** 없음

- [ ] **Step 1: main 브랜치 최신 상태 확인 후 브랜치 생성**

```bash
git checkout main
git pull origin main
git checkout -b fix/status-revert-and-badge-text
```

Expected: `Switched to a new branch 'fix/status-revert-and-badge-text'`

---

## Task 2: 백엔드 — `Task.changeStatus()` 제한 해제

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java`
- Create: `backend/src/test/java/com/nudgeup/backend/domain/task/entity/TaskTest.java`

- [ ] **Step 1: 테스트 파일 생성 (failing)**

`backend/src/test/java/com/nudgeup/backend/domain/task/entity/TaskTest.java` 를 새로 만든다:

```java
package com.nudgeup.backend.domain.task.entity;

import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class TaskTest {

    private Task makeTask(TaskStatus status) {
        Task task = Task.builder()
                .teamId(1L)
                .title("테스트 작업")
                .createdById(1L)
                .assigneeId(1L)
                .dueDate(LocalDate.now().plusDays(7))
                .build();
        if (status == TaskStatus.IN_PROGRESS) {
            task.changeStatus(TaskStatus.IN_PROGRESS);
        } else if (status == TaskStatus.COMPLETED) {
            task.changeStatus(TaskStatus.IN_PROGRESS);
            task.changeStatus(TaskStatus.COMPLETED);
        }
        return task;
    }

    @Test
    void 완료된_작업을_진행중으로_되돌릴_수_있다() {
        Task task = makeTask(TaskStatus.COMPLETED);
        assertThatCode(() -> task.changeStatus(TaskStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void 완료된_작업을_대기로_되돌리면_예외() {
        Task task = makeTask(TaskStatus.COMPLETED);
        assertThatThrownBy(() -> task.changeStatus(TaskStatus.PENDING))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TASK_ALREADY_COMPLETED);
    }

    @Test
    void 진행중에서_완료로_변경_가능() {
        Task task = makeTask(TaskStatus.IN_PROGRESS);
        assertThatCode(() -> task.changeStatus(TaskStatus.COMPLETED))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
cd backend
./gradlew test --tests "com.nudgeup.backend.domain.task.entity.TaskTest" 2>&1 | tail -20
```

Expected: `완료된_작업을_진행중으로_되돌릴_수_있다` FAILED (CustomException 발생으로 인해)

- [ ] **Step 3: `Task.changeStatus()` 수정**

`backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java` 의 `changeStatus` 메서드를 아래로 교체한다:

```java
public void changeStatus(TaskStatus newStatus) {
    if (this.status == TaskStatus.COMPLETED && newStatus != TaskStatus.IN_PROGRESS) {
        throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
    }
    if (newStatus == TaskStatus.IN_PROGRESS) {
        this.startedAt = LocalDateTime.now();
    } else if (newStatus == TaskStatus.COMPLETED) {
        this.completedAt = LocalDateTime.now();
    }
    this.status = newStatus;
}
```

- [ ] **Step 4: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.nudgeup.backend.domain.task.entity.TaskTest"
```

Expected: 3개 테스트 모두 PASSED

- [ ] **Step 5: 전체 테스트 확인**

```bash
./gradlew test 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java \
        backend/src/test/java/com/nudgeup/backend/domain/task/entity/TaskTest.java
git commit -m "fix: 완료된 작업을 진행중으로 되돌릴 수 있도록 changeStatus 제한 완화"
```

---

## Task 3: 앱 — "기한초과" 배지 텍스트 수정

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/adapter/TaskListAdapter.java`

현재 `onBindViewHolder` 내 배지 텍스트 결정 로직:
```java
// 현재 (line ~91):
} else if (isUrgent) {
    badgeText      = "마감임박";
```

`isUrgent`는 `!"COMPLETED".equals(task.getStatus()) && dday <= 3` 으로 정의됨.
dday < 0 (기한 초과)도 isUrgent에 포함되어 "마감임박"이 표시됨.
두 경우를 분리해 별도 텍스트를 보여줘야 한다.

- [ ] **Step 1: `onBindViewHolder` 첫 부분의 상태 변수 추가**

`TaskListAdapter.java` 의 `onBindViewHolder` 상단, 기존 변수 선언 바로 아래에 `isOverdue` 추가:

```java
TaskResponse task = getItem(position);
long dday        = calcDday(task.getDueDate());
boolean isUrgent    = !"COMPLETED".equals(task.getStatus()) && dday >= 0 && dday <= 3;
boolean isOverdue   = !"COMPLETED".equals(task.getStatus()) && dday < 0;
boolean isCompleted = "COMPLETED".equals(task.getStatus());
```

기존 코드:
```java
boolean isUrgent    = !"COMPLETED".equals(task.getStatus()) && dday <= 3;
```
이 한 줄을 위 3줄로 교체한다.

- [ ] **Step 2: 상태 바 색상 로직 수정**

기존:
```java
if (isCompleted)                              statusColor = 0xFF2ECC71;
else if (isUrgent)                            statusColor = 0xFFFF6B35;
else if ("IN_PROGRESS".equals(task.getStatus())) statusColor = 0xFF7B3FE4;
else                                          statusColor = 0xFF4A4A6A;
```

교체:
```java
if (isCompleted)                                 statusColor = 0xFF2ECC71;
else if (isOverdue)                              statusColor = 0xFFFF6B35;
else if (isUrgent)                               statusColor = 0xFFFF6B35;
else if ("IN_PROGRESS".equals(task.getStatus())) statusColor = 0xFF7B3FE4;
else                                             statusColor = 0xFF4A4A6A;
```

- [ ] **Step 3: 배지 텍스트/색상 로직 수정**

기존 배지 결정 블록:
```java
if (isCompleted) {
    badgeText      = "완료";
    badgeTextColor = 0xFF2ECC71;
    badgeBgRes     = R.drawable.bg_badge_complete;
} else if (isUrgent) {
    badgeText      = "마감임박";
    badgeTextColor = 0xFFFF6B35;
    badgeBgRes     = R.drawable.bg_badge_urgent;
} else if ("IN_PROGRESS".equals(task.getStatus())) {
    badgeText      = "진행중";
    badgeTextColor = 0xFF7B3FE4;
    badgeBgRes     = R.drawable.bg_badge_progress;
} else {
    badgeText      = "대기";
    badgeTextColor = 0xFF8888AA;
    badgeBgRes     = R.drawable.bg_badge_pending;
}
```

교체:
```java
if (isCompleted) {
    badgeText      = "완료";
    badgeTextColor = 0xFF2ECC71;
    badgeBgRes     = R.drawable.bg_badge_complete;
} else if (isOverdue) {
    badgeText      = "기한초과";
    badgeTextColor = 0xFFFF6B35;
    badgeBgRes     = R.drawable.bg_badge_urgent;
} else if (isUrgent) {
    badgeText      = "마감임박";
    badgeTextColor = 0xFFFF6B35;
    badgeBgRes     = R.drawable.bg_badge_urgent;
} else if ("IN_PROGRESS".equals(task.getStatus())) {
    badgeText      = "진행중";
    badgeTextColor = 0xFF7B3FE4;
    badgeBgRes     = R.drawable.bg_badge_progress;
} else {
    badgeText      = "대기";
    badgeTextColor = 0xFF8888AA;
    badgeBgRes     = R.drawable.bg_badge_pending;
}
```

- [ ] **Step 4: 앱 빌드 확인**

```bash
cd app
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/adapter/TaskListAdapter.java
git commit -m "fix: 기한 초과 작업 배지 텍스트를 '기한초과'로 수정"
```

---

## Task 4: PR 생성

- [ ] **Step 1: 원격 브랜치 push**

```bash
git push -u origin fix/status-revert-and-badge-text
```

- [ ] **Step 2: PR 생성**

```bash
gh pr create \
  --base main \
  --title "fix: 완료 작업 복구 허용 및 기한초과 배지 텍스트 수정" \
  --body "## Summary
- COMPLETED 상태에서 IN_PROGRESS로 되돌릴 수 있도록 백엔드 changeStatus() 제한 완화
- 기한이 지난 작업의 배지 텍스트를 '마감임박'에서 '기한초과'로 분리

## Test plan
- [ ] 완료 처리된 작업의 상태 배지 클릭 → 진행중 선택 → 상태 변경 확인
- [ ] dday < 0인 작업의 배지가 '기한초과'로 표시되는지 확인
- [ ] dday 0~3인 미완료 작업의 배지가 여전히 '마감임박'으로 표시되는지 확인"
```
