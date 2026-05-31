# feat/nudge-inbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 메인 화면 하단 벨 아이콘을 클릭 가능한 버튼으로 바꾸고, 내가 받은 독촉 알림 수신함 화면을 구현하며, 독촉 N회 카운트를 "내가 받은 최근 3일 독촉 수"로 변경한다.

**Architecture:** 백엔드에 assignee 기준 독촉 로그 조회 API 2종 추가(`GET /api/v1/nudges/received`, `GET /api/v1/nudges/received/count`). 앱에서 새 `NudgeInboxActivity`와 `NudgeLogAdapter`를 추가하고, `MainViewModel`의 nudgeCount를 서버 호출로 전환한다.

**Tech Stack:** Java 17, Spring Boot 3, Android (Java), Retrofit2, RecyclerView

---

## File Map

**백엔드:**
| 파일 | 변경 유형 |
|---|---|
| `backend/.../nudge/repository/NudgeLogRepository.java` | Modify |
| `backend/.../nudge/service/NudgeService.java` | Modify |
| `backend/.../nudge/dto/response/NudgeLogResponse.java` | Modify |
| `backend/.../nudge/controller/NudgeController.java` | Modify |

**앱:**
| 파일 | 변경 유형 |
|---|---|
| `app/.../res/layout/activity_main.xml` | Modify |
| `app/.../ui/main/MainActivity.java` | Modify |
| `app/.../ui/main/MainViewModel.java` | Modify |
| `app/.../data/api/NudgeApi.java` | Modify |
| `app/.../data/repository/NudgeRepository.java` | Modify |
| `app/.../data/dto/response/NudgeLogItemResponse.java` | Create |
| `app/.../ui/nudge/NudgeInboxActivity.java` | Create |
| `app/.../res/layout/activity_nudge_inbox.xml` | Create |
| `app/.../res/layout/item_nudge_log.xml` | Create |
| `app/.../ui/nudge/NudgeLogAdapter.java` | Create |

---

## Task 1: 브랜치 생성

- [ ] **Step 1: main에서 브랜치 생성**

```bash
git checkout main
git pull origin main
git checkout -b feat/nudge-inbox
```

Expected: `Switched to a new branch 'feat/nudge-inbox'`

---

## Task 2: 백엔드 — NudgeLogResponse에 팀 정보 추가

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/dto/response/NudgeLogResponse.java`

- [ ] **Step 1: NudgeLogResponse에 teamId, teamName 필드 추가**

파일 전체를 아래로 교체:

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
    private Long teamId;
    private String teamName;
    private UserResponse sentTo;
    private UserResponse sentBy;
    private String sentAt;

    public static NudgeLogResponse from(NudgeLog log, String taskTitle,
                                        Long teamId, String teamName,
                                        UserResponse sentTo, UserResponse sentBy) {
        return NudgeLogResponse.builder()
                .nudgeLogId(log.getId())
                .taskId(log.getTaskId())
                .taskTitle(taskTitle)
                .teamId(teamId)
                .teamName(teamName)
                .sentTo(sentTo)
                .sentBy(sentBy)
                .sentAt(log.getSentAt() != null ? log.getSentAt().toString() : null)
                .build();
    }

    // 기존 getNudgeLogs()에서 사용하던 4-param 버전 (하위 호환)
    public static NudgeLogResponse from(NudgeLog log, String taskTitle,
                                        UserResponse sentTo, UserResponse sentBy) {
        return from(log, taskTitle, null, null, sentTo, sentBy);
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
cd backend
./gradlew compileJava 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/dto/response/NudgeLogResponse.java
git commit -m "feat: NudgeLogResponse에 teamId, teamName 필드 추가"
```

---

## Task 3: 백엔드 — NudgeLogRepository 수신자 기준 쿼리 추가

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java`

- [ ] **Step 1: 수신자 기준 쿼리 추가**

`NudgeLogRepository.java` 전체를 아래로 교체:

```java
package com.nudgeup.backend.domain.nudge.repository;

import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NudgeLogRepository extends JpaRepository<NudgeLog, Long> {

    List<NudgeLog> findByTaskId(Long taskId);

    long countByTaskId(Long taskId);

    @Query("SELECT n FROM NudgeLog n WHERE n.taskId IN :taskIds ORDER BY n.sentAt DESC")
    List<NudgeLog> findByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    // 내가 assignee인 작업들에 대해 받은 독촉 로그 (최신순)
    @Query("SELECT n FROM NudgeLog n WHERE n.taskId IN " +
           "(SELECT t.id FROM Task t WHERE t.assigneeId = :userId) " +
           "ORDER BY n.sentAt DESC")
    List<NudgeLog> findReceivedByUserId(@Param("userId") Long userId);

    // 내가 받은 독촉 중 since 이후 개수
    @Query("SELECT COUNT(n) FROM NudgeLog n WHERE n.taskId IN " +
           "(SELECT t.id FROM Task t WHERE t.assigneeId = :userId) " +
           "AND n.sentAt >= :since")
    long countReceivedByUserIdSince(@Param("userId") Long userId,
                                    @Param("since") LocalDateTime since);
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
./gradlew compileJava 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java
git commit -m "feat: NudgeLogRepository에 수신자 기준 조회 쿼리 추가"
```

---

## Task 4: 백엔드 — NudgeService 수신 관련 메서드 추가

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/service/NudgeService.java`

NudgeService에 TeamRepository를 추가하여 팀명을 조회해야 한다.

- [ ] **Step 1: NudgeService에 TeamRepository 의존성 추가**

`NudgeService.java` 의 import 목록에 추가:
```java
import com.nudgeup.backend.domain.team.entity.Team;
import com.nudgeup.backend.domain.team.repository.TeamRepository;
```

필드에 추가:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NudgeService {

    private final NudgeLogRepository nudgeLogRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final TeamRepository teamRepository;   // 추가
```

- [ ] **Step 2: getReceivedNudgeLogs() 메서드 추가**

`getNudgeLogs()` 메서드 아래에 추가:

```java
public List<NudgeLogResponse> getReceivedNudgeLogs(Long userId) {
    List<NudgeLog> logs = nudgeLogRepository.findReceivedByUserId(userId);

    return logs.stream()
            .map(log -> {
                Task task = taskRepository.findById(log.getTaskId())
                        .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
                User sentByUser = userRepository.findById(log.getSentById())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                UserResponse sentByResponse = UserResponse.from(sentByUser);

                UserResponse sentToResponse = null;
                if (task.getAssigneeId() != null) {
                    sentToResponse = userRepository.findById(task.getAssigneeId())
                            .map(UserResponse::from).orElse(null);
                }

                String teamName = teamRepository.findById(task.getTeamId())
                        .map(team -> team.getName())
                        .orElse("");

                return NudgeLogResponse.from(
                        log, task.getTitle(),
                        task.getTeamId(), teamName,
                        sentToResponse, sentByResponse);
            })
            .collect(Collectors.toList());
}

public long getReceivedNudgeCount(Long userId, int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    return nudgeLogRepository.countReceivedByUserIdSince(userId, since);
}
```

`import java.time.LocalDateTime;` 가 없으면 import 추가.

- [ ] **Step 3: 컴파일 확인**

```bash
./gradlew compileJava 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/service/NudgeService.java
git commit -m "feat: NudgeService에 getReceivedNudgeLogs, getReceivedNudgeCount 추가"
```

---

## Task 5: 백엔드 — NudgeController 수신 API 엔드포인트 추가

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/controller/NudgeController.java`

현재 NudgeController는 `@RequestMapping("/api/v1/teams/{teamId}")`. 수신 API는 cross-team이므로 같은 컨트롤러에 추가 매핑으로 추가한다.

- [ ] **Step 1: 수신 API 엔드포인트 2개 추가**

`NudgeController.java` 전체를 아래로 교체:

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
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NudgeController {

    private final NudgeService nudgeService;

    @PostMapping("/api/v1/teams/{teamId}/tasks/{taskId}/nudge")
    public ResponseEntity<ApiResponse<NudgeResponse>> sendNudge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("독촉 알림을 발송했습니다.",
                nudgeService.sendNudge(userId, teamId, taskId)));
    }

    @GetMapping("/api/v1/teams/{teamId}/nudge-logs")
    public ResponseEntity<ApiResponse<List<NudgeLogResponse>>> getNudgeLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestParam(required = false) Long taskId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("독촉 로그 조회 성공",
                nudgeService.getNudgeLogs(userId, teamId, taskId)));
    }

    @GetMapping("/api/v1/nudges/received")
    public ResponseEntity<ApiResponse<List<NudgeLogResponse>>> getReceivedNudgeLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("받은 독촉 로그 조회 성공",
                nudgeService.getReceivedNudgeLogs(userId)));
    }

    @GetMapping("/api/v1/nudges/received/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getReceivedNudgeCount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "3") int days) {
        Long userId = userDetails.getUser().getId();
        long count = nudgeService.getReceivedNudgeCount(userId, days);
        return ResponseEntity.ok(ApiResponse.success("받은 독촉 수 조회 성공",
                Map.of("count", count)));
    }
}
```

- [ ] **Step 2: SecurityConfig에서 새 경로 허용 확인**

```bash
grep -r "nudges" backend/src/main/java/ | grep -i "permit\|security\|config"
```

`/api/v1/nudges/**` 경로가 JWT 인증 필요 경로에 포함되어 있는지 확인. 기존 설정이 `/api/v1/**`를 JWT 필요로 설정되어 있다면 자동 적용됨. 별도 예외 처리 불필요.

- [ ] **Step 3: 전체 테스트**

```bash
./gradlew test 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/nudge/controller/NudgeController.java
git commit -m "feat: 받은 독촉 로그/카운트 API 엔드포인트 추가"
```

---

## Task 6: 앱 — NudgeLogItemResponse DTO 생성

**Files:**
- Create: `app/app/src/main/java/com/nudgeup/app/data/dto/response/NudgeLogItemResponse.java`

- [ ] **Step 1: 파일 생성**

```java
package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;
import com.nudgeup.app.domain.User;

public class NudgeLogItemResponse {

    @SerializedName("nudgeLogId")
    private long id;
    private long taskId;
    private String taskTitle;
    private long teamId;
    private String teamName;
    private User sentTo;
    private User sentBy;
    private String sentAt;

    public long getId()        { return id; }
    public long getTaskId()    { return taskId; }
    public String getTaskTitle() { return taskTitle; }
    public long getTeamId()    { return teamId; }
    public String getTeamName() { return teamName; }
    public User getSentTo()    { return sentTo; }
    public User getSentBy()    { return sentBy; }
    public String getSentAt()  { return sentAt; }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
cd app
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/dto/response/NudgeLogItemResponse.java
git commit -m "feat: NudgeLogItemResponse DTO 추가"
```

---

## Task 7: 앱 — NudgeApi, NudgeRepository 확장

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/data/api/NudgeApi.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/NudgeRepository.java`

- [ ] **Step 1: NudgeApi.java 확장**

전체 교체:

```java
package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NudgeApi {

    @POST("teams/{teamId}/tasks/{taskId}/nudge")
    Call<ApiResponse<Void>> sendNudge(
            @Path("teamId") long teamId,
            @Path("taskId") long taskId);

    @GET("nudges/received")
    Call<ApiResponse<List<NudgeLogItemResponse>>> getReceivedNudgeLogs();

    @GET("nudges/received/count")
    Call<ApiResponse<Map<String, Long>>> getReceivedNudgeCount(
            @Query("days") int days);
}
```

- [ ] **Step 2: NudgeRepository.java 확장**

전체 교체:

```java
package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.NudgeApi;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import com.nudgeup.app.global.network.ApiClient;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class NudgeRepository {

    private final NudgeApi nudgeApi;

    public NudgeRepository() {
        this.nudgeApi = ApiClient.getInstance().create(NudgeApi.class);
    }

    public Call<ApiResponse<Void>> sendNudge(long teamId, long taskId) {
        return nudgeApi.sendNudge(teamId, taskId);
    }

    public Call<ApiResponse<List<NudgeLogItemResponse>>> getReceivedNudgeLogs() {
        return nudgeApi.getReceivedNudgeLogs();
    }

    public Call<ApiResponse<Map<String, Long>>> getReceivedNudgeCount(int days) {
        return nudgeApi.getReceivedNudgeCount(days);
    }
}
```

- [ ] **Step 3: 빌드 확인**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/api/NudgeApi.java \
        app/app/src/main/java/com/nudgeup/app/data/repository/NudgeRepository.java
git commit -m "feat: NudgeApi, NudgeRepository에 수신 독촉 API 메서드 추가"
```

---

## Task 8: 앱 — MainViewModel nudgeCount 서버 호출로 전환

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainViewModel.java`

- [ ] **Step 1: loadReceivedNudgeCount() 메서드 추가**

`MainViewModel.java` 의 `sendNudge()` 메서드 아래에 추가:

```java
public void loadReceivedNudgeCount() {
    nudgeRepository.getReceivedNudgeCount(3).enqueue(
            new Callback<ApiResponse<java.util.Map<String, Long>>>() {
                @Override
                public void onResponse(
                        Call<ApiResponse<java.util.Map<String, Long>>> call,
                        Response<ApiResponse<java.util.Map<String, Long>>> response) {
                    if (response.isSuccessful() && response.body() != null
                            && response.body().isSuccess()) {
                        java.util.Map<String, Long> data = response.body().getData();
                        Long count = data != null ? data.get("count") : 0L;
                        nudgeCount.setValue(count != null ? count.intValue() : 0);
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<java.util.Map<String, Long>>> call,
                                      Throwable t) {
                    // 카운트 갱신 실패는 무시
                }
            });
}
```

- [ ] **Step 2: selectTeam()과 loadTasks()에서 카운트 갱신 호출 추가**

`selectTeam()` 메서드 끝에 `loadReceivedNudgeCount();` 추가:
```java
public void selectTeam(long teamId) {
    // ... 기존 코드 ...
    selectedMemberId = -1L;
    loadMembers(teamId);
    loadTasks(teamId);
    loadReceivedNudgeCount();  // 추가
}
```

- [ ] **Step 3: 빌드 확인**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/MainViewModel.java
git commit -m "feat: nudgeCount를 서버에서 받은 최근 3일 독촉 수로 변경"
```

---

## Task 9: 앱 — item_nudge_log.xml 레이아웃 생성

**Files:**
- Create: `app/app/src/main/res/layout/item_nudge_log.xml`

- [ ] **Step 1: 레이아웃 파일 생성**

`app/app/src/main/res/layout/item_nudge_log.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="16dp"
    android:gravity="center_vertical">

    <!-- 벨 아이콘 -->
    <ImageView
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:src="@drawable/ic_bell"
        android:scaleType="fitCenter"
        android:layout_marginEnd="12dp"
        android:contentDescription="독촉 알림"/>

    <!-- 텍스트 영역 -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <!-- 팀명 + 작업명 -->
        <TextView
            android:id="@+id/tv_nudge_team_task"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="13sp"
            android:textStyle="bold"
            android:textColor="#F0F0FF"
            android:layout_marginBottom="3dp"/>

        <!-- "보낸사람님이 독촉을 보냈습니다" -->
        <TextView
            android:id="@+id/tv_nudge_message"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textColor="#9999BB"/>

    </LinearLayout>

    <!-- 시간 -->
    <TextView
        android:id="@+id/tv_nudge_time"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="11sp"
        android:textColor="#666688"
        android:layout_marginStart="8dp"/>

</LinearLayout>
```

- [ ] **Step 2: 구분선 drawable이 없으면 넘어감 (bg_dropdown 재사용 가능)**

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/res/layout/item_nudge_log.xml
git commit -m "feat: item_nudge_log.xml 레이아웃 추가"
```

---

## Task 10: 앱 — NudgeLogAdapter 생성

**Files:**
- Create: `app/app/src/main/java/com/nudgeup/app/ui/nudge/NudgeLogAdapter.java`

- [ ] **Step 1: 디렉토리 생성 확인 후 파일 생성**

```bash
mkdir -p app/app/src/main/java/com/nudgeup/app/ui/nudge
```

`NudgeLogAdapter.java`:

```java
package com.nudgeup.app.ui.nudge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NudgeLogAdapter extends ListAdapter<NudgeLogItemResponse, NudgeLogAdapter.ViewHolder> {

    public NudgeLogAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nudge_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NudgeLogItemResponse item = getItem(position);

        String teamName = item.getTeamName() != null ? item.getTeamName() : "";
        String taskTitle = item.getTaskTitle() != null ? item.getTaskTitle() : "";
        holder.tvTeamTask.setText("[" + teamName + "]  " + taskTitle);

        String senderName = (item.getSentBy() != null && item.getSentBy().getName() != null)
                ? item.getSentBy().getName() : "누군가";
        holder.tvMessage.setText(senderName + "님이 독촉을 보냈습니다");

        holder.tvTime.setText(formatRelativeTime(item.getSentAt()));
    }

    private String formatRelativeTime(String sentAtStr) {
        if (sentAtStr == null) return "";
        try {
            // "2026-05-31T14:30:00" 또는 "2026-05-31 14:30:00" 형식 처리 (minSdk 24 호환)
            String normalized = sentAtStr.replace("T", " ");
            if (normalized.length() > 19) normalized = normalized.substring(0, 19);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date sentAt = sdf.parse(normalized);
            if (sentAt == null) return "";
            long diffMs = new Date().getTime() - sentAt.getTime();
            long minutes = diffMs / 60000;
            if (minutes < 1)    return "방금 전";
            if (minutes < 60)   return minutes + "분 전";
            long hours = minutes / 60;
            if (hours < 24)     return hours + "시간 전";
            return (hours / 24) + "일 전";
        } catch (ParseException e) {
            return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTeamTask, tvMessage, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeamTask = itemView.findViewById(R.id.tv_nudge_team_task);
            tvMessage  = itemView.findViewById(R.id.tv_nudge_message);
            tvTime     = itemView.findViewById(R.id.tv_nudge_time);
        }
    }

    private static final DiffUtil.ItemCallback<NudgeLogItemResponse> DIFF =
            new DiffUtil.ItemCallback<NudgeLogItemResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull NudgeLogItemResponse o, @NonNull NudgeLogItemResponse n) {
                    return o.getId() == n.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull NudgeLogItemResponse o, @NonNull NudgeLogItemResponse n) {
                    return o.getId() == n.getId();
                }
            };
}
```

- [ ] **Step 2: 빌드 확인**

```bash
cd app
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/nudge/NudgeLogAdapter.java
git commit -m "feat: NudgeLogAdapter 추가"
```

---

## Task 11: 앱 — activity_nudge_inbox.xml 생성

**Files:**
- Create: `app/app/src/main/res/layout/activity_nudge_inbox.xml`

- [ ] **Step 1: 레이아웃 파일 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#13131F"
    android:fitsSystemWindows="true">

    <!-- 상단 바 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="8dp"
        android:paddingEnd="16dp"
        android:background="#1A1A2E">

        <ImageButton
            android:id="@+id/btn_back"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@drawable/ic_close"
            android:background="@android:color/transparent"
            android:scaleType="center"
            android:contentDescription="뒤로가기"/>

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="독촉 알림 수신함"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="#F0F0FF"
            android:layout_marginStart="4dp"/>

    </LinearLayout>

    <!-- 목록 -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_nudge_inbox"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingTop="4dp"
        android:paddingBottom="16dp"/>

    <!-- 빈 상태 -->
    <TextView
        android:id="@+id/tv_empty"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:gravity="center"
        android:text="아직 받은 독촉이 없어요"
        android:textSize="15sp"
        android:textColor="#666688"
        android:visibility="gone"/>

</LinearLayout>
```

- [ ] **Step 2: 커밋**

```bash
git add app/app/src/main/res/layout/activity_nudge_inbox.xml
git commit -m "feat: activity_nudge_inbox.xml 레이아웃 추가"
```

---

## Task 12: 앱 — NudgeInboxActivity 생성

**Files:**
- Create: `app/app/src/main/java/com/nudgeup/app/ui/nudge/NudgeInboxActivity.java`

- [ ] **Step 1: NudgeInboxActivity 생성**

```java
package com.nudgeup.app.ui.nudge;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import com.nudgeup.app.data.repository.NudgeRepository;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NudgeInboxActivity extends AppCompatActivity {

    private NudgeLogAdapter adapter;
    private final NudgeRepository nudgeRepository = new NudgeRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudge_inbox);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_nudge_inbox);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NudgeLogAdapter();
        rv.setAdapter(adapter);

        loadNudgeLogs();
    }

    private void loadNudgeLogs() {
        nudgeRepository.getReceivedNudgeLogs().enqueue(
                new Callback<ApiResponse<List<NudgeLogItemResponse>>>() {
                    @Override
                    public void onResponse(
                            Call<ApiResponse<List<NudgeLogItemResponse>>> call,
                            Response<ApiResponse<List<NudgeLogItemResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            List<NudgeLogItemResponse> data = response.body().getData();
                            boolean isEmpty = (data == null || data.isEmpty());
                            adapter.submitList(data);
                            findViewById(R.id.rv_nudge_inbox).setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                            findViewById(R.id.tv_empty).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<List<NudgeLogItemResponse>>> call,
                                         Throwable t) {
                        // 네트워크 오류 무시 (화면에 빈 상태 표시)
                    }
                });
    }
}
```

- [ ] **Step 2: AndroidManifest.xml에 Activity 등록**

`app/app/src/main/AndroidManifest.xml` 에 아래 줄 추가 (다른 `<activity>` 태그 옆에):

```xml
<activity android:name=".ui.nudge.NudgeInboxActivity" />
```

- [ ] **Step 3: 빌드 확인**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/nudge/NudgeInboxActivity.java \
        app/app/src/main/AndroidManifest.xml
git commit -m "feat: NudgeInboxActivity 추가 및 Manifest 등록"
```

---

## Task 13: 앱 — activity_main.xml 벨 아이콘 → 버튼 + MainActivity 연결

**Files:**
- Modify: `app/app/src/main/res/layout/activity_main.xml`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java`

- [ ] **Step 1: activity_main.xml 벨 ImageView → ImageButton으로 교체**

`activity_main.xml` 하단 바 영역에서 기존 벨 `ImageView` 를 찾아 `ImageButton`으로 교체한다.

기존 (line ~289):
```xml
<ImageView
    android:layout_width="18dp"
    android:layout_height="18dp"
    android:src="@drawable/ic_bell"
    android:scaleType="fitCenter"
    android:contentDescription="독촉 알림"/>
```

교체:
```xml
<ImageButton
    android:id="@+id/btn_nudge_inbox"
    android:layout_width="30dp"
    android:layout_height="30dp"
    android:src="@drawable/ic_bell"
    android:background="@android:color/transparent"
    android:scaleType="fitCenter"
    android:contentDescription="독촉 알림 수신함"/>
```

- [ ] **Step 2: MainActivity.java에 뷰 참조 및 클릭 리스너 추가**

`MainActivity.java` 의 `// View 참조` 블록에 추가:
```java
View btnNudgeInbox = findViewById(R.id.btn_nudge_inbox);
```

버튼 이벤트 블록에 클릭 리스너 추가:
```java
btnNudgeInbox.setOnClickListener(v ->
        startActivity(new Intent(this, NudgeInboxActivity.class)));
```

import 추가:
```java
import com.nudgeup.app.ui.nudge.NudgeInboxActivity;
```

- [ ] **Step 3: 빌드 확인**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/res/layout/activity_main.xml \
        app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java
git commit -m "feat: 벨 아이콘을 버튼으로 교체하고 NudgeInboxActivity로 연결"
```

---

## Task 14: 전체 테스트 및 PR 생성

- [ ] **Step 1: 백엔드 전체 테스트**

```bash
cd backend
./gradlew test 2>&1 | tail -5
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
git push -u origin feat/nudge-inbox
```

- [ ] **Step 4: PR 생성**

```bash
gh pr create \
  --base main \
  --title "feat: 독촉 알림 수신함 및 받은 독촉 카운트 구현" \
  --body "## Summary
- 메인 화면 하단 벨 아이콘 → 클릭 가능한 ImageButton으로 교체
- NudgeInboxActivity: 전체 팀 통합 독촉 수신함 (보낸사람 + 작업명 템플릿, 상대시간 표시)
- 독촉 N회 카운트를 내가 받은 최근 3일 독촉 수로 변경 (서버 API 호출)
- 백엔드: GET /api/v1/nudges/received, GET /api/v1/nudges/received/count 추가

## Test plan
- [ ] 벨 버튼 클릭 시 NudgeInboxActivity 열리는지 확인
- [ ] 수신함에 독촉 로그가 '[팀명]  [작업명] / 보낸사람님이 독촉을 보냈습니다 / N분 전' 형태로 표시되는지 확인
- [ ] 독촉이 없을 때 '아직 받은 독촉이 없어요' 빈 상태 표시 확인
- [ ] 메인 화면 하단 독촉 N회 카운트가 내가 받은 최근 3일 수로 표시되는지 확인
- [ ] 팀 전환 시 카운트가 갱신되는지 확인"
```
