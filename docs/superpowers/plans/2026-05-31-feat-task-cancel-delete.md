# feat/task-cancel-delete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 모든 작업에 폐기(CANCELLED) 기능을 추가하고, COMPLETED/CANCELLED 상태가 3일 이상 지속된 작업을 자동으로 DB에서 삭제한다.

**Architecture:** 백엔드에 `CANCELLED` enum 값 추가, `cancelledAt` 타임스탬프, 폐기 권한 확장(생성자 포함), 매일 새벽 3시 실행되는 `TaskCleanupScheduler`. 앱에서는 작업 카드에 X 버튼을 추가해 CANCELLED 상태로 PATCH 호출한다.

**Tech Stack:** Java 17, Spring Boot 3, `@Scheduled`, Android (Java), Retrofit2

---

## 전제 조건

- `fix/status-revert-and-badge-text` 브랜치가 main에 머지된 상태에서 시작
- 또는 fix 브랜치 작업 완료 후 해당 브랜치 기반으로 시작해도 무방

---

## File Map

| 파일 | 변경 유형 |
|---|---|
| `backend/.../task/entity/TaskStatus.java` | Modify |
| `backend/.../task/entity/Task.java` | Modify |
| `backend/.../global/exception/ErrorCode.java` | Modify |
| `backend/.../task/service/TaskService.java` | Modify |
| `backend/.../task/repository/TaskRepository.java` | Modify |
| `backend/.../nudge/repository/NudgeLogRepository.java` | Modify |
| `backend/.../global/schedule/TaskCleanupScheduler.java` | Create |
| `backend/.../task/entity/TaskTest.java` | Modify |
| `app/.../res/layout/item_task_card.xml` | Modify |
| `app/.../ui/main/adapter/TaskListAdapter.java` | Modify |
| `app/.../ui/main/MainViewModel.java` | Modify |

---

## Task 1: 브랜치 생성

- [ ] **Step 1: main에서 브랜치 생성**

```bash
git checkout main
git pull origin main
git checkout -b feat/task-cancel-delete
```

Expected: `Switched to a new branch 'feat/task-cancel-delete'`

---

## Task 2: 백엔드 — TaskStatus에 CANCELLED 추가

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/entity/TaskStatus.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/global/exception/ErrorCode.java`

- [ ] **Step 1: TaskStatus.java 수정**

전체 파일 내용을 아래로 교체:

```java
package com.nudgeup.backend.domain.task.entity;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
```

- [ ] **Step 2: ErrorCode.java에 TASK_ALREADY_CANCELLED 추가**

`ErrorCode.java` 의 `// Task` 섹션에 한 줄 추가:

```java
// Task
TASK_NOT_FOUND(404, "작업을 찾을 수 없습니다."),
TASK_ALREADY_COMPLETED(400, "이미 완료된 작업입니다."),
TASK_ALREADY_CANCELLED(400, "이미 폐기된 작업입니다."),
NO_TASK_PERMISSION(403, "담당자 또는 팀장만 상태를 변경할 수 있습니다."),
```

- [ ] **Step 3: 백엔드 컴파일 확인**

```bash
cd backend
./gradlew compileJava 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/entity/TaskStatus.java \
        backend/src/main/java/com/nudgeup/backend/global/exception/ErrorCode.java
git commit -m "feat: TaskStatus에 CANCELLED 추가 및 ErrorCode 등록"
```

---

## Task 3: 백엔드 — Task 엔티티에 cancelledAt 추가 및 changeStatus 수정

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java`
- Modify: `backend/src/test/java/com/nudgeup/backend/domain/task/entity/TaskTest.java`

- [ ] **Step 1: 테스트 추가 (failing)**

`TaskTest.java` 에 아래 테스트를 추가:

```java
@Test
void 진행중_작업을_폐기할_수_있다() {
    Task task = makeTask(TaskStatus.IN_PROGRESS);
    assertThatCode(() -> task.changeStatus(TaskStatus.CANCELLED))
            .doesNotThrowAnyException();
    assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
}

@Test
void 완료된_작업을_폐기할_수_있다() {
    Task task = makeTask(TaskStatus.COMPLETED);
    assertThatCode(() -> task.changeStatus(TaskStatus.CANCELLED))
            .doesNotThrowAnyException();
    assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
}

@Test
void 폐기된_작업은_다른_상태로_변경_불가() {
    Task task = makeTask(TaskStatus.IN_PROGRESS);
    task.changeStatus(TaskStatus.CANCELLED);
    assertThatThrownBy(() -> task.changeStatus(TaskStatus.IN_PROGRESS))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.TASK_ALREADY_CANCELLED);
}

@Test
void 완료된_작업을_진행중으로_되돌릴_수_있다() {
    // fix 브랜치에서 이미 있다면 중복 제거 가능
    Task task = makeTask(TaskStatus.COMPLETED);
    assertThatCode(() -> task.changeStatus(TaskStatus.IN_PROGRESS))
            .doesNotThrowAnyException();
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

```bash
./gradlew test --tests "com.nudgeup.backend.domain.task.entity.TaskTest" 2>&1 | tail -10
```

Expected: `진행중_작업을_폐기할_수_있다` 등 FAILED

- [ ] **Step 3: Task.java 수정**

`Task.java` 에 `cancelledAt` 필드 추가 및 `changeStatus()` 수정.

필드 추가 — 기존 `completedAt` 아래에 추가:
```java
@Column(name = "completed_at")
private LocalDateTime completedAt;

@Column(name = "cancelled_at")
private LocalDateTime cancelledAt;
```

`changeStatus()` 메서드 전체 교체:
```java
public void changeStatus(TaskStatus newStatus) {
    if (this.status == TaskStatus.CANCELLED) {
        throw new CustomException(ErrorCode.TASK_ALREADY_CANCELLED);
    }
    if (this.status == TaskStatus.COMPLETED
            && newStatus != TaskStatus.IN_PROGRESS
            && newStatus != TaskStatus.CANCELLED) {
        throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
    }
    if (newStatus == TaskStatus.IN_PROGRESS) {
        this.startedAt = LocalDateTime.now();
    } else if (newStatus == TaskStatus.COMPLETED) {
        this.completedAt = LocalDateTime.now();
    } else if (newStatus == TaskStatus.CANCELLED) {
        this.cancelledAt = LocalDateTime.now();
    }
    this.status = newStatus;
}
```

- [ ] **Step 4: 테스트 재실행 — 통과 확인**

```bash
./gradlew test --tests "com.nudgeup.backend.domain.task.entity.TaskTest"
```

Expected: 모든 테스트 PASSED

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/entity/Task.java \
        backend/src/test/java/com/nudgeup/backend/domain/task/entity/TaskTest.java
git commit -m "feat: Task 엔티티에 cancelledAt 추가 및 CANCELLED 전환 로직 구현"
```

---

## Task 4: 백엔드 — TaskService 폐기 권한 확장

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/service/TaskService.java`

현재 `updateTaskStatus()` 는 `isAssignee || isLeader` 만 허용한다.  
폐기(CANCELLED) 전환 시에는 생성자(`createdById`)도 허용해야 한다.

- [ ] **Step 1: `updateTaskStatus()` 권한 검증 수정**

`TaskService.java` 의 `updateTaskStatus()` 메서드에서 아래 블록을 교체한다.

기존:
```java
boolean isAssignee = userId.equals(task.getAssigneeId());
boolean isLeader = member.getRole() == TeamRole.LEADER;

if (!isAssignee && !isLeader) {
    throw new CustomException(ErrorCode.NO_TASK_PERMISSION);
}
```

교체:
```java
boolean isAssignee  = userId.equals(task.getAssigneeId());
boolean isCreatedBy = userId.equals(task.getCreatedById());
boolean isLeader    = member.getRole() == TeamRole.LEADER;

boolean isCancelRequest = request.getStatus() == TaskStatus.CANCELLED;
boolean hasPermission = isAssignee || isLeader || (isCancelRequest && isCreatedBy);

if (!hasPermission) {
    throw new CustomException(ErrorCode.NO_TASK_PERMISSION);
}
```

- [ ] **Step 2: 전체 테스트 확인**

```bash
./gradlew test 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/service/TaskService.java
git commit -m "feat: 작업 폐기 시 생성자도 권한 허용"
```

---

## Task 5: 백엔드 — 자동 만료 스케줄러

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/repository/TaskRepository.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java`
- Create: `backend/src/main/java/com/nudgeup/backend/global/schedule/TaskCleanupScheduler.java`

- [ ] **Step 1: TaskRepository에 만료 대상 조회 메서드 추가**

`TaskRepository.java` 에 아래 메서드 추가:

```java
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

@Query("SELECT t FROM Task t WHERE " +
       "(t.status = :completed AND t.completedAt < :cutoff) OR " +
       "(t.status = :cancelled AND t.cancelledAt < :cutoff)")
List<Task> findExpiredTasks(@Param("completed") TaskStatus completed,
                             @Param("cancelled") TaskStatus cancelled,
                             @Param("cutoff") LocalDateTime cutoff);
```

- [ ] **Step 2: NudgeLogRepository에 일괄 삭제 메서드 추가**

`NudgeLogRepository.java` 에 아래 메서드 추가:

```java
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

@Modifying
@Query("DELETE FROM NudgeLog n WHERE n.taskId IN :taskIds")
void deleteByTaskIdIn(@Param("taskIds") List<Long> taskIds);
```

- [ ] **Step 3: TaskCleanupScheduler 생성**

`backend/src/main/java/com/nudgeup/backend/global/schedule/TaskCleanupScheduler.java` 를 새로 만든다:

```java
package com.nudgeup.backend.global.schedule;

import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCleanupScheduler {

    private final TaskRepository taskRepository;
    private final NudgeLogRepository nudgeLogRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        List<Task> expiredTasks = taskRepository.findExpiredTasks(
                TaskStatus.COMPLETED, TaskStatus.CANCELLED, cutoff);

        if (expiredTasks.isEmpty()) {
            return;
        }

        List<Long> taskIds = expiredTasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());

        nudgeLogRepository.deleteByTaskIdIn(taskIds);
        taskRepository.deleteAllById(taskIds);

        log.info("만료된 작업 {}건 자동 삭제 완료", taskIds.size());
    }
}
```

- [ ] **Step 4: `@EnableScheduling` 활성화 확인**

메인 애플리케이션 클래스 또는 설정 클래스에 `@EnableScheduling` 어노테이션이 있는지 확인한다.

```bash
grep -r "EnableScheduling" backend/src/main/java/
```

없으면 메인 클래스에 추가:
```java
@SpringBootApplication
@EnableScheduling  // 이 줄 추가
public class NudgeUpApplication { ... }
```

- [ ] **Step 5: 전체 빌드 확인**

```bash
./gradlew compileJava 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/repository/TaskRepository.java \
        backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java \
        backend/src/main/java/com/nudgeup/backend/global/schedule/TaskCleanupScheduler.java
git commit -m "feat: COMPLETED/CANCELLED 3일 후 자동 삭제 스케줄러 추가"
```

---

## Task 6: 앱 — item_task_card.xml에 폐기 버튼 추가

**Files:**
- Modify: `app/app/src/main/res/layout/item_task_card.xml`

- [ ] **Step 1: btnCancel ImageButton 추가**

`item_task_card.xml` 의 `<!-- 액션 버튼 -->` 주석 바로 위, 즉 `btnAction` 앞에 아래 버튼을 삽입한다:

```xml
<!-- 폐기 버튼 -->
<ImageButton
    android:id="@+id/btn_cancel_task"
    android:layout_width="36dp"
    android:layout_height="36dp"
    android:src="@drawable/ic_close"
    android:background="@android:color/transparent"
    android:scaleType="center"
    android:contentDescription="작업 폐기"/>
```

`ic_close` drawable이 없으면 대신 `@drawable/ic_edit` 임시 사용 후 나중에 교체.

실제로 ic_close가 없으면 아래 명령으로 확인 후 있는 아이콘으로 대체:
```bash
ls app/app/src/main/res/drawable/ | grep -i close
ls app/app/src/main/res/drawable/ | grep -i cancel
ls app/app/src/main/res/drawable/ | grep -i delete
```

- [ ] **Step 2: 앱 빌드 확인**

```bash
cd app
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/res/layout/item_task_card.xml
git commit -m "feat: 작업 카드에 폐기 버튼 UI 추가"
```

---

## Task 7: 앱 — TaskListAdapter 폐기 버튼 로직 + CANCELLED 렌더링

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/adapter/TaskListAdapter.java`

- [ ] **Step 1: OnCancelClickListener 인터페이스 추가**

`TaskListAdapter.java` 의 기존 인터페이스 선언 바로 아래에 추가:

```java
public interface OnCancelClickListener {
    void onCancelClick(long taskId);
}
```

- [ ] **Step 2: 생성자 필드 추가**

```java
private final OnNudgeClickListener nudgeListener;
private final OnStatusChangeListener statusListener;
private final OnCancelClickListener cancelListener;  // 추가
private final boolean isMyTaskMode;
```

생성자 변경:
```java
public TaskListAdapter(OnNudgeClickListener nudgeListener,
                       OnStatusChangeListener statusListener,
                       OnCancelClickListener cancelListener,
                       boolean isMyTaskMode) {
    super(DIFF_CALLBACK);
    this.nudgeListener  = nudgeListener;
    this.statusListener = statusListener;
    this.cancelListener = cancelListener;
    this.isMyTaskMode   = isMyTaskMode;
}
```

- [ ] **Step 3: ViewHolder에 btnCancel 추가**

`ViewHolder` 클래스 내부:
```java
static class ViewHolder extends RecyclerView.ViewHolder {
    View vStatusBar;
    TextView tvStatusBadge, tvDday, tvTaskTitle, tvAssigneeAvatar, tvAssigneeName;
    ImageButton btnAction;
    ImageButton btnCancel;  // 추가
    final GradientDrawable avatarBg;

    ViewHolder(@NonNull View itemView) {
        super(itemView);
        vStatusBar        = itemView.findViewById(R.id.v_status_bar);
        tvStatusBadge     = itemView.findViewById(R.id.tv_status_badge);
        tvDday            = itemView.findViewById(R.id.tv_dday);
        tvTaskTitle       = itemView.findViewById(R.id.tv_task_title);
        tvAssigneeAvatar  = itemView.findViewById(R.id.tv_assignee_avatar);
        tvAssigneeName    = itemView.findViewById(R.id.tv_assignee_name);
        btnAction         = itemView.findViewById(R.id.btn_action);
        btnCancel         = itemView.findViewById(R.id.btn_cancel_task);  // 추가
        avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        tvAssigneeAvatar.setBackground(avatarBg);
    }
}
```

- [ ] **Step 4: onBindViewHolder에 CANCELLED 렌더링 추가**

기존 `boolean isCompleted = "COMPLETED".equals(task.getStatus());` 아래에 추가:

```java
boolean isCancelled = "CANCELLED".equals(task.getStatus());
```

상태 바 색상 블록에 CANCELLED 추가:
```java
if (isCompleted)                                 statusColor = 0xFF2ECC71;
else if (isCancelled)                            statusColor = 0xFF555577;
else if (isOverdue)                              statusColor = 0xFFFF6B35;
else if (isUrgent)                               statusColor = 0xFFFF6B35;
else if ("IN_PROGRESS".equals(task.getStatus())) statusColor = 0xFF7B3FE4;
else                                             statusColor = 0xFF4A4A6A;
```

배지 텍스트 블록에 CANCELLED 추가 (첫 번째 케이스로):
```java
if (isCancelled) {
    badgeText      = "중단";
    badgeTextColor = 0xFF888899;
    badgeBgRes     = R.drawable.bg_badge_pending;
} else if (isCompleted) {
    ...
```

- [ ] **Step 5: 폐기 버튼 가시성 및 클릭 처리**

`onBindViewHolder` 의 `// ── 액션 버튼 ──` 섹션 이후에 추가:

```java
// ── 폐기 버튼 ──
if (isCancelled) {
    holder.btnCancel.setVisibility(View.GONE);
} else {
    holder.btnCancel.setVisibility(View.VISIBLE);
    holder.btnCancel.setAlpha(0.6f);
    holder.btnCancel.setOnClickListener(v -> {
        new android.app.AlertDialog.Builder(v.getContext())
                .setTitle("작업 폐기")
                .setMessage("'" + task.getTitle() + "' 작업을 폐기하시겠습니까?\n폐기된 작업은 3일 후 자동으로 삭제됩니다.")
                .setPositiveButton("폐기", (dialog, which) -> cancelListener.onCancelClick(task.getId()))
                .setNegativeButton("취소", null)
                .show();
    });
}
```

- [ ] **Step 6: 앱 빌드 확인**

```bash
cd app
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/adapter/TaskListAdapter.java
git commit -m "feat: TaskListAdapter에 CANCELLED 렌더링 및 폐기 버튼 로직 추가"
```

---

## Task 8: 앱 — MainViewModel에 cancelTask 추가 및 MainActivity 연결

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainViewModel.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java`

- [ ] **Step 1: MainViewModel에 cancelTask 추가**

`updateTaskStatus()` 메서드 아래에 추가:

```java
public void cancelTask(long taskId) {
    updateTaskStatus(taskId, "CANCELLED");
}
```

- [ ] **Step 2: MainActivity에서 어댑터 생성자 수정**

`MainActivity.java` 의 `teamTaskAdapter` 초기화 블록을 수정한다.

기존:
```java
teamTaskAdapter = new TaskListAdapter(
        taskId -> viewModel.sendNudge(taskId),
        (taskId, status) -> viewModel.updateTaskStatus(taskId, status),
        false
);
```

변경:
```java
teamTaskAdapter = new TaskListAdapter(
        taskId -> viewModel.sendNudge(taskId),
        (taskId, status) -> viewModel.updateTaskStatus(taskId, status),
        taskId -> viewModel.cancelTask(taskId),
        false
);
```

`myTaskAdapter` 초기화 블록도 수정:

기존:
```java
myTaskAdapter = new TaskListAdapter(
        taskId -> {
            Intent intent = new Intent(this, CreateTaskActivity.class);
            intent.putExtra("teamId", viewModel.getCurrentTeam().getValue() != null
                    ? viewModel.getCurrentTeam().getValue().getId() : -1L);
            startActivity(intent);
        },
        (taskId, status) -> viewModel.updateTaskStatus(taskId, status),
        true
);
```

변경 (세 번째 인자 `cancelListener` 추가):
```java
myTaskAdapter = new TaskListAdapter(
        taskId -> {
            Intent intent = new Intent(this, CreateTaskActivity.class);
            intent.putExtra("teamId", viewModel.getCurrentTeam().getValue() != null
                    ? viewModel.getCurrentTeam().getValue().getId() : -1L);
            startActivity(intent);
        },
        (taskId, status) -> viewModel.updateTaskStatus(taskId, status),
        taskId -> viewModel.cancelTask(taskId),
        true
);
```

- [ ] **Step 3: 앱 빌드 확인**

```bash
cd app
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/MainViewModel.java \
        app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java
git commit -m "feat: MainViewModel cancelTask 추가 및 어댑터 연결"
```

---

## Task 9: 전체 테스트 및 PR 생성

- [ ] **Step 1: 백엔드 전체 테스트**

```bash
cd backend
./gradlew test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: 앱 전체 빌드**

```bash
cd app
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 원격 push**

```bash
git push -u origin feat/task-cancel-delete
```

- [ ] **Step 4: PR 생성**

```bash
gh pr create \
  --base main \
  --title "feat: 작업 폐기(CANCELLED) 기능 및 3일 자동 만료 추가" \
  --body "## Summary
- TaskStatus에 CANCELLED 추가 — 모든 상태의 작업을 폐기 가능
- 폐기 권한: 담당자, 생성자, 팀장
- 작업 카드에 X 버튼 추가 (확인 다이얼로그 후 CANCELLED PATCH)
- COMPLETED/CANCELLED 상태가 3일 이상 지속된 작업 매일 새벽 3시 자동 삭제

## Test plan
- [ ] 작업 카드 폐기 버튼 클릭 → 확인 다이얼로그 → 상태 '중단'으로 변경 확인
- [ ] CANCELLED 상태 배지가 '중단'으로 표시되는지 확인
- [ ] 생성자(담당자 아닌)가 폐기 시 API 성공 확인
- [ ] 팀장이 모든 작업 폐기 가능한지 확인
- [ ] TaskCleanupScheduler가 스프링 컨텍스트에서 정상 빈 등록되는지 확인 (로그)"
```
