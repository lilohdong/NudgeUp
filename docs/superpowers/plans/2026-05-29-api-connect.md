# API 연결 및 작업 정렬 기능 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android 앱의 모든 ViewModel 목업을 제거하고 실제 백엔드 API와 연결하며, 작업 목록에 담당자순/마감일순 정렬 드롭박스를 추가한다.

**Architecture:** Retrofit + OkHttp 기반 네트워크 레이어는 이미 완성돼 있다. DTO 필드명 불일치(`@SerializedName`)와 API URL 오류를 먼저 수정한 뒤, 각 ViewModel에서 목업 블록을 실제 Repository 호출로 교체한다. 정렬 기능은 `MainViewModel`에 `SortMode` enum을 추가하고 `activity_main.xml` 두 섹션 헤더에 Spinner를 붙인다.

**Tech Stack:** Java 17, Retrofit 2, Gson, Android ViewModel + LiveData, Firebase Messaging, JUnit 4

---

## 파일 구조

| 작업 | 파일 |
|------|------|
| 수정 | `data/dto/request/RegisterRequest.java` |
| 수정 | `data/dto/request/CreateTaskRequest.java` |
| 수정 | `data/dto/request/TeamSettingsRequest.java` |
| **신규** | `data/dto/request/MemberPermissionRequest.java` |
| **신규** | `data/dto/request/UpdateStatusRequest.java` |
| **신규** | `data/dto/request/FcmTokenRequest.java` |
| 수정 | `data/dto/response/TeamResponse.java` |
| 수정 | `data/dto/response/TaskResponse.java` |
| 수정 | `data/dto/response/MemberResponse.java` |
| 수정 | `domain/User.java` |
| **신규** | `data/dto/response/TeamDetailResponse.java` |
| **신규** | `data/dto/response/UserResponse.java` |
| 수정 | `data/api/AuthApi.java` |
| 수정 | `data/api/TaskApi.java` |
| 수정 | `data/api/NudgeApi.java` |
| 수정 | `data/api/TeamApi.java` |
| **신규** | `data/api/UserApi.java` |
| 수정 | `data/repository/AuthRepository.java` |
| 수정 | `data/repository/TaskRepository.java` |
| 수정 | `data/repository/NudgeRepository.java` |
| 수정 | `data/repository/TeamRepository.java` |
| **신규** | `data/repository/UserRepository.java` |
| 수정 | `ui/auth/LoginViewModel.java` |
| 수정 | `ui/auth/RegisterViewModel.java` |
| 수정 | `ui/main/MainViewModel.java` |
| 수정 | `ui/task/CreateTaskViewModel.java` |
| 수정 | `ui/team/TeamSettingsViewModel.java` |
| 수정 | `global/fcm/NudgeFcmService.java` |
| 수정 | `res/layout/activity_main.xml` |
| 수정 | `ui/main/MainActivity.java` |
| **신규** | `src/test/.../MainViewModelSortTest.java` |

모든 Java 파일은 `app/app/src/main/java/com/nudgeup/app/` 기준.

---

## Task 1: Request DTO 수정 및 신규 추가

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/data/dto/request/RegisterRequest.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/dto/request/CreateTaskRequest.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/dto/request/TeamSettingsRequest.java`
- Create: `app/app/src/main/java/com/nudgeup/app/data/dto/request/MemberPermissionRequest.java`
- Create: `app/app/src/main/java/com/nudgeup/app/data/dto/request/UpdateStatusRequest.java`
- Create: `app/app/src/main/java/com/nudgeup/app/data/dto/request/FcmTokenRequest.java`

- [ ] **Step 1: RegisterRequest — `name` → `username` 필드명 수정**

API 명세: `{ "username": "홍길동", "email": "...", "password": "..." }`

```java
// RegisterRequest.java
package com.nudgeup.app.data.dto.request;

public class RegisterRequest {
    private String email;
    private String password;
    private String username;

    public RegisterRequest(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }
}
```

- [ ] **Step 2: CreateTaskRequest — `teamId` 제거, `description` 추가**

API 명세: `POST /teams/{teamId}/tasks` — teamId는 path param이므로 body에서 제거. description 필드 추가.

```java
// CreateTaskRequest.java
package com.nudgeup.app.data.dto.request;

public class CreateTaskRequest {
    private String title;
    private String description;
    private Long assigneeId;
    private String dueDate;

    public CreateTaskRequest(String title, String description, Long assigneeId, String dueDate) {
        this.title = title;
        this.description = description;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
    }
}
```

- [ ] **Step 3: MemberPermissionRequest 신규 생성**

TeamSettingsRequest 내부 배열 항목. API 명세: `{ "userId": 2, "canNudge": true }`

```java
// MemberPermissionRequest.java
package com.nudgeup.app.data.dto.request;

public class MemberPermissionRequest {
    private long userId;
    private boolean canNudge;

    public MemberPermissionRequest(long userId, boolean canNudge) {
        this.userId = userId;
        this.canNudge = canNudge;
    }
}
```

- [ ] **Step 4: TeamSettingsRequest 재작성**

API 명세: `{ "newLeaderId": 2, "memberPermissions": [{ "userId": 2, "canNudge": true }] }`  
기존 `Map<Long, Boolean>` 방식은 JSON 배열과 맞지 않으므로 List로 변경.

```java
// TeamSettingsRequest.java
package com.nudgeup.app.data.dto.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamSettingsRequest {
    private Long newLeaderId;
    private List<MemberPermissionRequest> memberPermissions;

    public TeamSettingsRequest(Long newLeaderId, Map<Long, Boolean> nudgePermissionsMap) {
        this.newLeaderId = newLeaderId;
        this.memberPermissions = new ArrayList<>();
        if (nudgePermissionsMap != null) {
            for (Map.Entry<Long, Boolean> entry : nudgePermissionsMap.entrySet()) {
                this.memberPermissions.add(new MemberPermissionRequest(entry.getKey(), entry.getValue()));
            }
        }
    }
}
```

- [ ] **Step 5: UpdateStatusRequest 신규 생성**

API 명세: `PATCH /teams/{teamId}/tasks/{taskId}/status` body: `{ "status": "IN_PROGRESS" }`

```java
// UpdateStatusRequest.java
package com.nudgeup.app.data.dto.request;

public class UpdateStatusRequest {
    private String status;

    public UpdateStatusRequest(String status) {
        this.status = status;
    }
}
```

- [ ] **Step 6: FcmTokenRequest 신규 생성**

API 명세: `PUT /users/me/fcm-token` body: `{ "fcmToken": "..." }`

```java
// FcmTokenRequest.java
package com.nudgeup.app.data.dto.request;

public class FcmTokenRequest {
    private String fcmToken;

    public FcmTokenRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
```

- [ ] **Step 7: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/dto/request/
git commit -m "feat: request DTO 필드명 수정 및 신규 DTO 추가 (UpdateStatusRequest 등)"
```

---

## Task 2: Response DTO 수정 및 신규 추가

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/domain/User.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/dto/response/TeamResponse.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/dto/response/TaskResponse.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/dto/response/MemberResponse.java`
- Create: `app/app/src/main/java/com/nudgeup/app/data/dto/response/TeamDetailResponse.java`
- Create: `app/app/src/main/java/com/nudgeup/app/data/dto/response/UserResponse.java`

Gson은 `@SerializedName` 어노테이션으로 JSON 키 → Java 필드명 매핑을 처리한다. import: `com.google.gson.annotations.SerializedName`

- [ ] **Step 1: User 도메인 필드명 수정**

API 명세: `{ "userId": 1, "username": "홍길동", "color": "#A78BFA" }`

```java
// User.java
package com.nudgeup.app.domain;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("userId")
    private long id;
    private String email;
    @SerializedName("username")
    private String name;
    private String color;

    public User() {}

    public User(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getColor() { return color; }
}
```

- [ ] **Step 2: TeamResponse 필드명 수정 + myRole 추가**

API 명세(팀 목록): `{ "teamId": 1, "teamName": "...", "leaderId": 1, "memberCount": 4, "myRole": "LEADER" }`

```java
// TeamResponse.java
package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;

public class TeamResponse {
    @SerializedName("teamId")
    private long id;
    @SerializedName("teamName")
    private String name;
    private long leaderId;
    private int memberCount;
    private String myRole;

    public TeamResponse() {}

    public long getId() { return id; }
    public String getName() { return name; }
    public long getLeaderId() { return leaderId; }
    public int getMemberCount() { return memberCount; }
    public String getMyRole() { return myRole; }
}
```

- [ ] **Step 3: TaskResponse 필드명 수정**

API 명세: `{ "taskId": 10, "title": "...", "status": "...", "dueDate": "...", "assignee": {...} }`

```java
// TaskResponse.java
package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;
import com.nudgeup.app.domain.User;

public class TaskResponse {
    @SerializedName("taskId")
    private long id;
    private String title;
    private String status;
    private String dueDate;
    private User assignee;
    private User createdBy;

    public TaskResponse() {}

    public TaskResponse(long id, String title, String status, String dueDate, User assignee) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.dueDate = dueDate;
        this.assignee = assignee;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getDueDate() { return dueDate; }
    public User getAssignee() { return assignee; }
    public User getCreatedBy() { return createdBy; }
}
```

- [ ] **Step 4: MemberResponse 필드명 수정**

API 명세: `{ "userId": 1, "username": "홍길동", "color": "#A78BFA", "role": "LEADER", "canNudge": true }`

```java
// MemberResponse.java
package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;

public class MemberResponse {
    private long userId;
    @SerializedName("username")
    private String name;
    private String role;
    private boolean canNudge;
    private String color;

    public MemberResponse() {}

    public MemberResponse(long userId, String name, String role, boolean canNudge) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.canNudge = canNudge;
    }

    public long getUserId() { return userId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public boolean isCanNudge() { return canNudge; }
    public String getColor() { return color; }
}
```

- [ ] **Step 5: TeamDetailResponse 신규 생성**

`GET /teams/{teamId}` 응답: 팀 상세 + members 포함. 멤버 목록 조회 및 inviteCode 접근에 사용.

```java
// TeamDetailResponse.java
package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TeamDetailResponse {
    @SerializedName("teamId")
    private long id;
    @SerializedName("teamName")
    private String name;
    private long leaderId;
    private String inviteCode;
    private List<MemberResponse> members;

    public long getId() { return id; }
    public String getName() { return name; }
    public long getLeaderId() { return leaderId; }
    public String getInviteCode() { return inviteCode; }
    public List<MemberResponse> getMembers() { return members; }
}
```

- [ ] **Step 6: UserResponse 신규 생성**

회원가입 응답: `{ "userId": 1, "username": "홍길동", "email": "...", "color": "#A78BFA" }`

```java
// UserResponse.java
package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("userId")
    private long id;
    @SerializedName("username")
    private String username;
    private String email;
    private String color;

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getColor() { return color; }
}
```

- [ ] **Step 7: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/dto/response/ \
        app/app/src/main/java/com/nudgeup/app/domain/User.java
git commit -m "feat: response DTO @SerializedName 수정 및 TeamDetailResponse/UserResponse 추가"
```

---

## Task 3: API 인터페이스 수정 및 UserApi 신규 추가

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/data/api/AuthApi.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/api/TaskApi.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/api/NudgeApi.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/api/TeamApi.java`
- Create: `app/app/src/main/java/com/nudgeup/app/data/api/UserApi.java`

- [ ] **Step 1: AuthApi — register 반환 타입 수정, logout 추가**

회원가입은 토큰을 반환하지 않으므로 `UserResponse`로 변경. 로그아웃 엔드포인트 추가.

```java
// AuthApi.java
package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.LoginRequest;
import com.nudgeup.app.data.dto.request.RefreshRequest;
import com.nudgeup.app.data.dto.request.RegisterRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.AuthResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/register")
    Call<ApiResponse<UserResponse>> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<ApiResponse<AuthResponse>> refresh(@Body RefreshRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();
}
```

- [ ] **Step 2: TaskApi — URL 전면 수정, getMyTasks 추가**

현재 `@GET("tasks")` 등의 URL이 명세와 다르다. 모두 `/teams/{teamId}/tasks` 패턴으로 수정.

```java
// TaskApi.java
package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.CreateTaskRequest;
import com.nudgeup.app.data.dto.request.UpdateStatusRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TaskApi {

    @GET("teams/{teamId}/tasks")
    Call<ApiResponse<List<TaskResponse>>> getTasks(@Path("teamId") long teamId);

    @GET("teams/{teamId}/tasks/my")
    Call<ApiResponse<List<TaskResponse>>> getMyTasks(@Path("teamId") long teamId);

    @POST("teams/{teamId}/tasks")
    Call<ApiResponse<TaskResponse>> createTask(
            @Path("teamId") long teamId,
            @Body CreateTaskRequest request);

    @PATCH("teams/{teamId}/tasks/{taskId}/status")
    Call<ApiResponse<TaskResponse>> updateStatus(
            @Path("teamId") long teamId,
            @Path("taskId") long taskId,
            @Body UpdateStatusRequest request);
}
```

- [ ] **Step 3: NudgeApi — URL 수정**

현재 `POST nudges/{taskId}` → `POST teams/{teamId}/tasks/{taskId}/nudge`

```java
// NudgeApi.java
package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.response.ApiResponse;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface NudgeApi {

    @POST("teams/{teamId}/tasks/{taskId}/nudge")
    Call<ApiResponse<Void>> sendNudge(
            @Path("teamId") long teamId,
            @Path("taskId") long taskId);
}
```

- [ ] **Step 4: TeamApi — getMembers 제거, getTeamDetail 추가**

명세에 별도 `/members` 엔드포인트가 없다. 팀 상세(`GET /teams/{teamId}`)에 members가 포함됨.

```java
// TeamApi.java
package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.CreateTeamRequest;
import com.nudgeup.app.data.dto.request.TeamSettingsRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.dto.response.TeamResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TeamApi {

    @GET("teams")
    Call<ApiResponse<List<TeamResponse>>> getMyTeams();

    @POST("teams")
    Call<ApiResponse<TeamDetailResponse>> createTeam(@Body CreateTeamRequest request);

    @GET("teams/{teamId}")
    Call<ApiResponse<TeamDetailResponse>> getTeamDetail(@Path("teamId") long teamId);

    @PATCH("teams/{teamId}/settings")
    Call<ApiResponse<Void>> updateSettings(
            @Path("teamId") long teamId,
            @Body TeamSettingsRequest request);
}
```

- [ ] **Step 5: UserApi 신규 생성**

```java
// UserApi.java
package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.FcmTokenRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;

public interface UserApi {

    @GET("users/me")
    Call<ApiResponse<UserResponse>> getMe();

    @PUT("users/me/fcm-token")
    Call<ApiResponse<Void>> registerFcmToken(@Body FcmTokenRequest request);
}
```

- [ ] **Step 6: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/api/
git commit -m "feat: API 인터페이스 URL 수정 및 UserApi 추가"
```

---

## Task 4: Repository 수정 및 UserRepository 신규 추가

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/AuthRepository.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/TaskRepository.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/NudgeRepository.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/TeamRepository.java`
- Create: `app/app/src/main/java/com/nudgeup/app/data/repository/UserRepository.java`

- [ ] **Step 1: AuthRepository — register 반환 타입 수정, logout 추가**

```java
// AuthRepository.java
package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.AuthApi;
import com.nudgeup.app.data.dto.request.LoginRequest;
import com.nudgeup.app.data.dto.request.RegisterRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.AuthResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import com.nudgeup.app.data.local.TokenManager;
import com.nudgeup.app.global.network.ApiClient;
import retrofit2.Call;

public class AuthRepository {

    private final AuthApi authApi;
    private final TokenManager tokenManager;

    public AuthRepository(TokenManager tokenManager) {
        this.authApi = ApiClient.getInstance().create(AuthApi.class);
        this.tokenManager = tokenManager;
    }

    public Call<ApiResponse<AuthResponse>> login(String email, String password) {
        return authApi.login(new LoginRequest(email, password));
    }

    public Call<ApiResponse<UserResponse>> register(String email, String password, String username) {
        return authApi.register(new RegisterRequest(email, password, username));
    }

    public Call<ApiResponse<Void>> logout() {
        return authApi.logout();
    }

    public void saveTokens(String accessToken, String refreshToken) {
        tokenManager.saveTokens(accessToken, refreshToken);
    }

    public void saveUserId(long userId) {
        tokenManager.saveUserId(userId);
    }

    public void clearTokens() {
        tokenManager.clearTokens();
    }
}
```

- [ ] **Step 2: TaskRepository — teamId 파라미터 추가, getMyTasks 추가**

```java
// TaskRepository.java
package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.TaskApi;
import com.nudgeup.app.data.dto.request.CreateTaskRequest;
import com.nudgeup.app.data.dto.request.UpdateStatusRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.global.network.ApiClient;
import java.util.List;
import retrofit2.Call;

public class TaskRepository {

    private final TaskApi taskApi;

    public TaskRepository() {
        this.taskApi = ApiClient.getInstance().create(TaskApi.class);
    }

    public Call<ApiResponse<List<TaskResponse>>> getTasks(long teamId) {
        return taskApi.getTasks(teamId);
    }

    public Call<ApiResponse<List<TaskResponse>>> getMyTasks(long teamId) {
        return taskApi.getMyTasks(teamId);
    }

    public Call<ApiResponse<TaskResponse>> createTask(
            long teamId, String title, String description, Long assigneeId, String dueDate) {
        return taskApi.createTask(teamId, new CreateTaskRequest(title, description, assigneeId, dueDate));
    }

    public Call<ApiResponse<TaskResponse>> updateStatus(long teamId, long taskId, String status) {
        return taskApi.updateStatus(teamId, taskId, new UpdateStatusRequest(status));
    }
}
```

- [ ] **Step 3: NudgeRepository — teamId 파라미터 추가**

```java
// NudgeRepository.java
package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.NudgeApi;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.global.network.ApiClient;
import retrofit2.Call;

public class NudgeRepository {

    private final NudgeApi nudgeApi;

    public NudgeRepository() {
        this.nudgeApi = ApiClient.getInstance().create(NudgeApi.class);
    }

    public Call<ApiResponse<Void>> sendNudge(long teamId, long taskId) {
        return nudgeApi.sendNudge(teamId, taskId);
    }
}
```

- [ ] **Step 4: TeamRepository — getMembers → getTeamDetail 교체**

```java
// TeamRepository.java
package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.TeamApi;
import com.nudgeup.app.data.dto.request.CreateTeamRequest;
import com.nudgeup.app.data.dto.request.TeamSettingsRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.dto.response.TeamResponse;
import com.nudgeup.app.global.network.ApiClient;
import java.util.List;
import retrofit2.Call;

public class TeamRepository {

    private final TeamApi teamApi;

    public TeamRepository() {
        this.teamApi = ApiClient.getInstance().create(TeamApi.class);
    }

    public Call<ApiResponse<List<TeamResponse>>> getMyTeams() {
        return teamApi.getMyTeams();
    }

    public Call<ApiResponse<TeamDetailResponse>> createTeam(String name) {
        return teamApi.createTeam(new CreateTeamRequest(name));
    }

    public Call<ApiResponse<TeamDetailResponse>> getTeamDetail(long teamId) {
        return teamApi.getTeamDetail(teamId);
    }

    public Call<ApiResponse<Void>> updateSettings(long teamId, TeamSettingsRequest request) {
        return teamApi.updateSettings(teamId, request);
    }
}
```

- [ ] **Step 5: UserRepository 신규 생성**

```java
// UserRepository.java
package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.UserApi;
import com.nudgeup.app.data.dto.request.FcmTokenRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import com.nudgeup.app.global.network.ApiClient;
import retrofit2.Call;

public class UserRepository {

    private final UserApi userApi;

    public UserRepository() {
        this.userApi = ApiClient.getInstance().create(UserApi.class);
    }

    public Call<ApiResponse<UserResponse>> getMe() {
        return userApi.getMe();
    }

    public Call<ApiResponse<Void>> registerFcmToken(String token) {
        return userApi.registerFcmToken(new FcmTokenRequest(token));
    }
}
```

- [ ] **Step 6: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/repository/
git commit -m "feat: Repository 시그니처 수정 및 UserRepository 추가"
```

---

## Task 5: LoginViewModel + RegisterViewModel 목업 제거

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/auth/LoginViewModel.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/auth/RegisterViewModel.java`

- [ ] **Step 1: LoginViewModel — 목업 블록 제거, 실제 API 코드 주석 해제**

```java
// LoginViewModel.java
package com.nudgeup.app.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.AuthResponse;
import com.nudgeup.app.data.repository.AuthRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<UiResult> loginResult = new MutableLiveData<>();

    public LoginViewModel() {
        authRepository = new AuthRepository(NudgeUpApplication.getTokenManager());
    }

    public LiveData<UiResult> getLoginResult() { return loginResult; }

    public void login(String email, String password) {
        authRepository.login(email, password).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse data = response.body().getData();
                    authRepository.saveTokens(data.getAccessToken(), data.getRefreshToken());
                    if (data.getUser() != null) {
                        authRepository.saveUserId(data.getUser().getId());
                    }
                    loginResult.setValue(UiResult.success());
                } else {
                    loginResult.setValue(UiResult.fail("이메일 또는 비밀번호를 확인해주세요."));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                loginResult.setValue(UiResult.fail("네트워크 오류가 발생했습니다."));
            }
        });
    }

    public static class UiResult {
        private final boolean success;
        private final String message;

        private UiResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static UiResult success() { return new UiResult(true, null); }
        public static UiResult fail(String msg) { return new UiResult(false, msg); }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
```

- [ ] **Step 2: RegisterViewModel — 토큰 저장 제거, UserResponse로 반환 타입 수정**

회원가입 응답에는 토큰이 없다. 성공 시 로그인 화면으로 이동 신호만 emit.

```java
// RegisterViewModel.java
package com.nudgeup.app.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import com.nudgeup.app.data.repository.AuthRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<LoginViewModel.UiResult> registerResult = new MutableLiveData<>();

    public RegisterViewModel() {
        authRepository = new AuthRepository(NudgeUpApplication.getTokenManager());
    }

    public LiveData<LoginViewModel.UiResult> getRegisterResult() { return registerResult; }

    public void register(String email, String password, String name) {
        authRepository.register(email, password, name).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    registerResult.setValue(LoginViewModel.UiResult.success());
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "회원가입에 실패했습니다.";
                    registerResult.setValue(LoginViewModel.UiResult.fail(msg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                registerResult.setValue(LoginViewModel.UiResult.fail("네트워크 오류가 발생했습니다."));
            }
        });
    }
}
```

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/auth/
git commit -m "feat: LoginViewModel/RegisterViewModel 목업 제거 및 실제 API 연결"
```

---

## Task 6: MainViewModel — SortMode 추가 + 정렬 로직 + 단위 테스트 작성

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainViewModel.java`
- Create: `app/app/src/test/java/com/nudgeup/app/ui/main/MainViewModelSortTest.java`

이 Task에서는 정렬 로직만 추가한다 (목업 제거는 Task 7). 테스트 가능한 정적 메서드 형태로 구현하여 Android 없이 JUnit으로 검증.

- [ ] **Step 1: MainViewModel에 SortMode enum 및 sortTasksStatic 정적 메서드 추가**

기존 `private List<TaskResponse> sortTasks(List<TaskResponse> list)` 를 아래로 교체.

```java
// MainViewModel.java 에 추가/수정할 부분

// 파일 상단 import 추가:
import java.util.Comparator;

// 클래스 내부에 추가:
public enum SortMode {
    DEFAULT,    // 마감 임박 → 진행 중 → 대기 → 완료
    BY_PERSON,  // 담당자 이름 가나다순, null 담당자는 맨 뒤
    BY_DEADLINE // 마감일 빠른 순, null 마감일은 맨 뒤
}

private SortMode teamTasksSortMode = SortMode.DEFAULT;
private SortMode myTasksSortMode = SortMode.DEFAULT;

public void setTeamTasksSortMode(SortMode mode) {
    teamTasksSortMode = mode;
    applyTeamTasksFilter();
}

public void setMyTasksSortMode(SortMode mode) {
    myTasksSortMode = mode;
    applyMyTasksFilter();
}

// 기존 private sortTasks 대신 아래 정적 메서드로 교체
static List<TaskResponse> sortTasksStatic(List<TaskResponse> list, SortMode mode) {
    List<TaskResponse> sorted = new ArrayList<>(list);
    switch (mode) {
        case BY_PERSON:
            sorted.sort((a, b) -> {
                String nameA = (a.getAssignee() != null) ? a.getAssignee().getName() : null;
                String nameB = (b.getAssignee() != null) ? b.getAssignee().getName() : null;
                if (nameA == null && nameB == null) return 0;
                if (nameA == null) return 1;
                if (nameB == null) return -1;
                return nameA.compareTo(nameB);
            });
            break;
        case BY_DEADLINE:
            sorted.sort((a, b) -> {
                String dateA = a.getDueDate();
                String dateB = b.getDueDate();
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateA.compareTo(dateB); // yyyy-MM-dd 문자열 사전순 = 날짜순
            });
            break;
        default: // DEFAULT
            sorted.sort(Comparator.comparingInt(t -> getPriorityStatic(t)));
            break;
    }
    return sorted;
}

// 기존 getPriority를 정적 버전으로 복제
static int getPriorityStatic(TaskResponse task) {
    if ("COMPLETED".equals(task.getStatus())) return 3;
    // D-day 계산은 기존 calcDday 로직과 동일
    long dday = calcDdayStatic(task.getDueDate());
    if (dday <= 3) return 0;
    if ("IN_PROGRESS".equals(task.getStatus())) return 1;
    return 2;
}

static long calcDdayStatic(String dueDateStr) {
    if (dueDateStr == null) return 999L;
    try {
        String[] parts = dueDateStr.split("-");
        if (parts.length != 3) return 999L;
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1;
        int day = Integer.parseInt(parts[2]);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Calendar due = Calendar.getInstance();
        due.set(year, month, day, 0, 0, 0);
        due.set(Calendar.MILLISECOND, 0);
        long diffMs = due.getTimeInMillis() - today.getTimeInMillis();
        return diffMs / (24L * 60 * 60 * 1000);
    } catch (Exception e) {
        return 999L;
    }
}
```

기존 `private sortTasks(list)`, `getPriority(task)`, `calcDday(str)` 인스턴스 메서드는 제거하고 정적 버전으로 통합.

- [ ] **Step 2: 단위 테스트 파일 생성**

```java
// MainViewModelSortTest.java
package com.nudgeup.app.ui.main;

import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.domain.User;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class MainViewModelSortTest {

    private static final User USER_가 = new User(1, "가나다");
    private static final User USER_나 = new User(2, "나다라");

    private List<TaskResponse> makeTasks() {
        return Arrays.asList(
            new TaskResponse(1, "Task1", "PENDING",     "2026-06-10", USER_나),
            new TaskResponse(2, "Task2", "IN_PROGRESS", "2026-06-01", USER_가),
            new TaskResponse(3, "Task3", "COMPLETED",   null,         USER_가)
        );
    }

    @Test
    public void sortByDeadline_nullLast() {
        List<TaskResponse> sorted = MainViewModel.sortTasksStatic(
            makeTasks(), MainViewModel.SortMode.BY_DEADLINE);
        assertEquals("2026-06-01", sorted.get(0).getDueDate());
        assertEquals("2026-06-10", sorted.get(1).getDueDate());
        assertNull(sorted.get(2).getDueDate());
    }

    @Test
    public void sortByPerson_alphabetical() {
        List<TaskResponse> sorted = MainViewModel.sortTasksStatic(
            makeTasks(), MainViewModel.SortMode.BY_PERSON);
        assertEquals("가나다", sorted.get(0).getAssignee().getName());
        assertEquals("가나다", sorted.get(1).getAssignee().getName());
        assertEquals("나다라", sorted.get(2).getAssignee().getName());
    }

    @Test
    public void sortDefault_completedLast() {
        List<TaskResponse> sorted = MainViewModel.sortTasksStatic(
            makeTasks(), MainViewModel.SortMode.DEFAULT);
        assertEquals("COMPLETED", sorted.get(sorted.size() - 1).getStatus());
    }

    @Test
    public void sortByPerson_nullAssigneeLast() {
        List<TaskResponse> tasks = Arrays.asList(
            new TaskResponse(1, "A", "PENDING", "2026-06-01", null),
            new TaskResponse(2, "B", "PENDING", "2026-06-01", USER_가)
        );
        List<TaskResponse> sorted = MainViewModel.sortTasksStatic(tasks, MainViewModel.SortMode.BY_PERSON);
        assertEquals(USER_가.getName(), sorted.get(0).getAssignee().getName());
        assertNull(sorted.get(1).getAssignee());
    }
}
```

- [ ] **Step 3: 테스트 실행 — 통과 확인**

Android Studio에서: `app/src/test/` 디렉토리 우클릭 → Run Tests  
또는 터미널:
```bash
cd app
./gradlew test --tests "com.nudgeup.app.ui.main.MainViewModelSortTest"
```
Expected: 4개 테스트 PASS

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/MainViewModel.java \
        app/app/src/test/java/com/nudgeup/app/ui/main/MainViewModelSortTest.java
git commit -m "feat: MainViewModel SortMode 추가 및 정렬 로직 단위 테스트"
```

---

## Task 7: MainViewModel — 목업 전면 제거, 실제 API 연결

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainViewModel.java`

- [ ] **Step 1: MainViewModel 전체 재작성 (목업 → 실제 API)**

```java
// MainViewModel.java
package com.nudgeup.app.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.dto.response.TeamResponse;
import com.nudgeup.app.data.repository.NudgeRepository;
import com.nudgeup.app.data.repository.TaskRepository;
import com.nudgeup.app.data.repository.TeamRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {

    private final TeamRepository teamRepository = new TeamRepository();
    private final TaskRepository taskRepository = new TaskRepository();
    private final NudgeRepository nudgeRepository = new NudgeRepository();

    private final MutableLiveData<List<TeamResponse>> teams = new MutableLiveData<>();
    private final MutableLiveData<List<TaskResponse>> teamTasks = new MutableLiveData<>();
    private final MutableLiveData<List<TaskResponse>> myTasks = new MutableLiveData<>();
    private final MutableLiveData<List<MemberResponse>> members = new MutableLiveData<>();
    private final MutableLiveData<TeamResponse> currentTeam = new MutableLiveData<>();
    private final MutableLiveData<Boolean> nudgeResult = new MutableLiveData<>();
    private final MutableLiveData<Integer> nudgeCount = new MutableLiveData<>(0);

    private List<TaskResponse> allTasksCache = new ArrayList<>();
    private List<TaskResponse> myTasksCache = new ArrayList<>();
    private long selectedMemberId = -1L;

    private SortMode teamTasksSortMode = SortMode.DEFAULT;
    private SortMode myTasksSortMode = SortMode.DEFAULT;

    public enum SortMode { DEFAULT, BY_PERSON, BY_DEADLINE }

    public LiveData<List<TeamResponse>> getTeams() { return teams; }
    public LiveData<List<TaskResponse>> getTeamTasks() { return teamTasks; }
    public LiveData<List<TaskResponse>> getMyTasks() { return myTasks; }
    public LiveData<List<MemberResponse>> getMembers() { return members; }
    public LiveData<TeamResponse> getCurrentTeam() { return currentTeam; }
    public LiveData<Boolean> getNudgeResult() { return nudgeResult; }
    public LiveData<Integer> getNudgeCount() { return nudgeCount; }

    public void setTeamTasksSortMode(SortMode mode) {
        teamTasksSortMode = mode;
        applyTeamTasksFilter();
    }

    public void setMyTasksSortMode(SortMode mode) {
        myTasksSortMode = mode;
        applyMyTasksFilter();
    }

    public void loadTeams() {
        teamRepository.getMyTeams().enqueue(new Callback<ApiResponse<List<TeamResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TeamResponse>>> call, Response<ApiResponse<List<TeamResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TeamResponse> list = response.body().getData();
                    teams.setValue(list);
                    if (list != null && !list.isEmpty()) {
                        selectTeam(list.get(0).getId());
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<TeamResponse>>> call, Throwable t) {}
        });
    }

    public void selectTeam(long teamId) {
        List<TeamResponse> teamList = teams.getValue();
        if (teamList != null) {
            for (TeamResponse t : teamList) {
                if (t.getId() == teamId) {
                    currentTeam.setValue(t);
                    break;
                }
            }
        }
        selectedMemberId = -1L;
        loadMembers(teamId);
        loadTasks(teamId);
    }

    public void loadTasks(long teamId) {
        taskRepository.getTasks(teamId).enqueue(new Callback<ApiResponse<List<TaskResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TaskResponse>>> call, Response<ApiResponse<List<TaskResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allTasksCache = new ArrayList<>(response.body().getData());
                    applyTeamTasksFilter();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<TaskResponse>>> call, Throwable t) {}
        });

        taskRepository.getMyTasks(teamId).enqueue(new Callback<ApiResponse<List<TaskResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TaskResponse>>> call, Response<ApiResponse<List<TaskResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    myTasksCache = new ArrayList<>(response.body().getData());
                    applyMyTasksFilter();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<TaskResponse>>> call, Throwable t) {}
        });
    }

    private void loadMembers(long teamId) {
        teamRepository.getTeamDetail(teamId).enqueue(new Callback<ApiResponse<TeamDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamDetailResponse>> call, Response<ApiResponse<TeamDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TeamDetailResponse detail = response.body().getData();
                    if (detail.getMembers() != null) {
                        members.setValue(detail.getMembers());
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {}
        });
    }

    public void filterByMember(long memberId) {
        this.selectedMemberId = memberId;
        applyTeamTasksFilter();
    }

    private void applyTeamTasksFilter() {
        List<TaskResponse> filtered;
        if (selectedMemberId == -1L) {
            filtered = new ArrayList<>(allTasksCache);
        } else {
            filtered = new ArrayList<>();
            for (TaskResponse t : allTasksCache) {
                if (t.getAssignee() != null && t.getAssignee().getId() == selectedMemberId) {
                    filtered.add(t);
                }
            }
        }
        teamTasks.setValue(sortTasksStatic(filtered, teamTasksSortMode));
    }

    private void applyMyTasksFilter() {
        myTasks.setValue(sortTasksStatic(new ArrayList<>(myTasksCache), myTasksSortMode));
    }

    public void sendNudge(long taskId) {
        TeamResponse team = currentTeam.getValue();
        if (team == null) return;
        nudgeRepository.sendNudge(team.getId(), taskId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Integer count = nudgeCount.getValue();
                    nudgeCount.setValue(count == null ? 1 : count + 1);
                    nudgeResult.setValue(true);
                } else {
                    nudgeResult.setValue(false);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                nudgeResult.setValue(false);
            }
        });
    }

    public void updateTaskStatus(long taskId, String newStatus) {
        TeamResponse team = currentTeam.getValue();
        if (team == null) return;
        taskRepository.updateStatus(team.getId(), taskId, newStatus).enqueue(new Callback<ApiResponse<TaskResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TaskResponse>> call, Response<ApiResponse<TaskResponse>> response) {
                if (response.isSuccessful()) {
                    loadTasks(team.getId()); // 목록 새로고침
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TaskResponse>> call, Throwable t) {}
        });
    }

    public boolean isCurrentUserLeader() {
        TeamResponse team = currentTeam.getValue();
        return team != null && "LEADER".equals(team.getMyRole());
    }

    static List<TaskResponse> sortTasksStatic(List<TaskResponse> list, SortMode mode) {
        List<TaskResponse> sorted = new ArrayList<>(list);
        switch (mode) {
            case BY_PERSON:
                sorted.sort((a, b) -> {
                    String nameA = (a.getAssignee() != null) ? a.getAssignee().getName() : null;
                    String nameB = (b.getAssignee() != null) ? b.getAssignee().getName() : null;
                    if (nameA == null && nameB == null) return 0;
                    if (nameA == null) return 1;
                    if (nameB == null) return -1;
                    return nameA.compareTo(nameB);
                });
                break;
            case BY_DEADLINE:
                sorted.sort((a, b) -> {
                    String dateA = a.getDueDate();
                    String dateB = b.getDueDate();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateA.compareTo(dateB);
                });
                break;
            default:
                sorted.sort(Comparator.comparingInt(MainViewModel::getPriorityStatic));
                break;
        }
        return sorted;
    }

    static int getPriorityStatic(TaskResponse task) {
        if ("COMPLETED".equals(task.getStatus())) return 3;
        long dday = calcDdayStatic(task.getDueDate());
        if (dday <= 3) return 0;
        if ("IN_PROGRESS".equals(task.getStatus())) return 1;
        return 2;
    }

    static long calcDdayStatic(String dueDateStr) {
        if (dueDateStr == null) return 999L;
        try {
            String[] parts = dueDateStr.split("-");
            if (parts.length != 3) return 999L;
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            Calendar due = Calendar.getInstance();
            due.set(year, month, day, 0, 0, 0);
            due.set(Calendar.MILLISECOND, 0);
            long diffMs = due.getTimeInMillis() - today.getTimeInMillis();
            return diffMs / (24L * 60 * 60 * 1000);
        } catch (Exception e) {
            return 999L;
        }
    }

    // MainActivity에서 D-day 표시용으로 호출하는 public 래퍼
    public long calcDday(String dueDateStr) {
        return calcDdayStatic(dueDateStr);
    }

    public int getPriority(TaskResponse task) {
        return getPriorityStatic(task);
    }
}
```

- [ ] **Step 2: 테스트 재실행 — 여전히 PASS인지 확인**

```bash
cd app
./gradlew test --tests "com.nudgeup.app.ui.main.MainViewModelSortTest"
```
Expected: 4개 PASS

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/MainViewModel.java
git commit -m "feat: MainViewModel 목업 제거 및 실제 API 연결"
```

---

## Task 8: CreateTaskViewModel 목업 제거

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/task/CreateTaskViewModel.java`

- [ ] **Step 1: CreateTaskViewModel 재작성**

```java
// CreateTaskViewModel.java
package com.nudgeup.app.ui.task;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.repository.TaskRepository;
import com.nudgeup.app.data.repository.TeamRepository;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTaskViewModel extends ViewModel {

    private final TaskRepository taskRepository = new TaskRepository();
    private final TeamRepository teamRepository = new TeamRepository();

    private final MutableLiveData<Boolean> createResult = new MutableLiveData<>();
    private final MutableLiveData<List<MemberResponse>> members = new MutableLiveData<>();

    public LiveData<Boolean> getCreateResult() { return createResult; }
    public LiveData<List<MemberResponse>> getMembers() { return members; }

    public void loadTeamMembers(long teamId) {
        teamRepository.getTeamDetail(teamId).enqueue(new Callback<ApiResponse<TeamDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamDetailResponse>> call, Response<ApiResponse<TeamDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TeamDetailResponse detail = response.body().getData();
                    if (detail.getMembers() != null) {
                        members.setValue(detail.getMembers());
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {}
        });
    }

    public void createTask(long teamId, String title, String description, Long assigneeId, String dueDate) {
        taskRepository.createTask(teamId, title, description, assigneeId, dueDate)
                .enqueue(new Callback<ApiResponse<TaskResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TaskResponse>> call, Response<ApiResponse<TaskResponse>> response) {
                createResult.setValue(response.isSuccessful() && response.body() != null && response.body().isSuccess());
            }
            @Override
            public void onFailure(Call<ApiResponse<TaskResponse>> call, Throwable t) {
                createResult.setValue(false);
            }
        });
    }
}
```

주의: `CreateTaskActivity`에서 `viewModel.createTask(title, assigneeId, teamId, dueDate)` 를 호출하는 부분을 `viewModel.createTask(teamId, title, description, assigneeId, dueDate)` 시그니처에 맞게 수정해야 한다. `CreateTaskActivity.java`를 열어 해당 호출부를 찾아 파라미터를 맞춘다.

- [ ] **Step 2: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/task/
git commit -m "feat: CreateTaskViewModel 목업 제거 및 실제 API 연결"
```

---

## Task 9: TeamSettingsViewModel 목업 제거

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/team/TeamSettingsViewModel.java`

- [ ] **Step 1: TeamSettingsViewModel 재작성**

```java
// TeamSettingsViewModel.java
package com.nudgeup.app.ui.team;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.data.dto.request.TeamSettingsRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.repository.TeamRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamSettingsViewModel extends ViewModel {

    private final TeamRepository teamRepository = new TeamRepository();

    private final MutableLiveData<List<MemberResponse>> members = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    private final MutableLiveData<String> inviteCode = new MutableLiveData<>();

    private final Map<Long, Boolean> pendingNudgePermissions = new HashMap<>();
    private Long pendingNewLeaderId = null;

    public LiveData<List<MemberResponse>> getMembers() { return members; }
    public LiveData<Boolean> getSaveResult() { return saveResult; }
    public LiveData<String> getInviteCode() { return inviteCode; }

    public void loadMembers(long teamId) {
        teamRepository.getTeamDetail(teamId).enqueue(new Callback<ApiResponse<TeamDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamDetailResponse>> call, Response<ApiResponse<TeamDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TeamDetailResponse detail = response.body().getData();
                    if (detail.getMembers() != null) {
                        members.setValue(detail.getMembers());
                    }
                    inviteCode.setValue(detail.getInviteCode());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {}
        });
    }

    public void setNudgePermission(long userId, boolean canNudge) {
        pendingNudgePermissions.put(userId, canNudge);
    }

    public void setNewLeader(long userId) {
        pendingNewLeaderId = userId;
    }

    public void saveSettings(long teamId) {
        TeamSettingsRequest request = new TeamSettingsRequest(pendingNewLeaderId, pendingNudgePermissions);
        teamRepository.updateSettings(teamId, request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                saveResult.setValue(response.isSuccessful());
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                saveResult.setValue(false);
            }
        });
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/team/TeamSettingsViewModel.java
git commit -m "feat: TeamSettingsViewModel 목업 제거 및 실제 API 연결"
```

---

## Task 10: FCM 토큰 서버 등록

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/global/fcm/NudgeFcmService.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/auth/LoginViewModel.java`

- [ ] **Step 1: NudgeFcmService.onNewToken() — 토큰 갱신 시 서버 등록**

```java
// NudgeFcmService.java — onNewToken 메서드만 교체
@Override
public void onNewToken(String token) {
    String accessToken = NudgeUpApplication.getTokenManager().getAccessToken();
    if (accessToken == null) return; // 로그인 상태가 아니면 스킵 (로그인 시 등록)
    new UserRepository().registerFcmToken(token).enqueue(new Callback<ApiResponse<Void>>() {
        @Override
        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
        @Override
        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
    });
}
```

파일 상단에 import 추가:
```java
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.repository.UserRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
```

- [ ] **Step 2: LoginViewModel — 로그인 성공 후 현재 FCM 토큰 서버 등록**

`login()` 메서드 내 `loginResult.setValue(UiResult.success())` 호출 직전에 FCM 토큰 등록 추가.

```java
// LoginViewModel.java — onResponse 내부, loginResult.setValue(UiResult.success()) 앞에 삽입
com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
    .addOnSuccessListener(fcmToken -> {
        if (fcmToken != null) {
            new UserRepository().registerFcmToken(fcmToken).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
            });
        }
    });
loginResult.setValue(UiResult.success());
```

파일 상단에 import 추가:
```java
import com.nudgeup.app.data.repository.UserRepository;
```

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/global/fcm/NudgeFcmService.java \
        app/app/src/main/java/com/nudgeup/app/ui/auth/LoginViewModel.java
git commit -m "feat: 로그인 및 FCM 토큰 갱신 시 서버에 fcm-token 등록"
```

---

## Task 11: UI — activity_main.xml 정렬 Spinner 추가 + MainActivity 연결

**Files:**
- Modify: `app/app/src/main/res/layout/activity_main.xml`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java`

- [ ] **Step 1: activity_main.xml — 두 섹션 헤더에 Spinner 추가**

`activity_main.xml`에서 팀원 전체 할일 섹션 헤더(`tv_team_task_count`가 있는 행)와 내 할일 섹션 헤더(`tv_my_task_count`가 있는 행)를 찾는다.

각 헤더 행의 레이아웃이 `LinearLayout`(horizontal)이면 끝에 아래 Spinner를 추가한다. `RelativeLayout`이면 `layout_alignParentEnd="true"`로 배치한다.

팀원 전체 할일 헤더 행에 추가:
```xml
<Spinner
    android:id="@+id/spinner_team_tasks_sort"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="center_vertical"
    android:minWidth="100dp" />
```

내 할일 헤더 행에 추가:
```xml
<Spinner
    android:id="@+id/spinner_my_tasks_sort"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="center_vertical"
    android:minWidth="100dp" />
```

- [ ] **Step 2: `res/values/strings.xml` — 정렬 옵션 문자열 배열 추가**

`strings.xml` 파일을 열어 `<resources>` 태그 안에 추가:
```xml
<string-array name="sort_options">
    <item>기본 정렬</item>
    <item>담당자 순</item>
    <item>마감일 순</item>
</string-array>
```

- [ ] **Step 3: MainActivity — Spinner 초기화 및 이벤트 연결**

`MainActivity.java`의 `onCreate` 내 "View 참조" 블록에 추가:
```java
Spinner spinnerTeamTasksSort = findViewById(R.id.spinner_team_tasks_sort);
Spinner spinnerMyTasksSort = findViewById(R.id.spinner_my_tasks_sort);
```

"버튼 이벤트" 블록 이후에 추가:
```java
// 정렬 Spinner 설정
ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
        this, R.array.sort_options, android.R.layout.simple_spinner_item);
sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

spinnerTeamTasksSort.setAdapter(sortAdapter);
spinnerTeamTasksSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
    @Override
    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
        MainViewModel.SortMode mode = indexToSortMode(position);
        viewModel.setTeamTasksSortMode(mode);
    }
    @Override
    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
});

spinnerMyTasksSort.setAdapter(sortAdapter);
spinnerMyTasksSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
    @Override
    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
        MainViewModel.SortMode mode = indexToSortMode(position);
        viewModel.setMyTasksSortMode(mode);
    }
    @Override
    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
});
```

`MainActivity` 클래스 내 private 헬퍼 메서드 추가:
```java
private MainViewModel.SortMode indexToSortMode(int position) {
    switch (position) {
        case 1: return MainViewModel.SortMode.BY_PERSON;
        case 2: return MainViewModel.SortMode.BY_DEADLINE;
        default: return MainViewModel.SortMode.DEFAULT;
    }
}
```

파일 상단에 import 추가:
```java
import android.widget.ArrayAdapter;
import android.widget.Spinner;
```

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/res/layout/activity_main.xml \
        app/app/src/main/res/values/strings.xml \
        app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java
git commit -m "feat: 메인 화면 작업 목록 정렬 드롭박스 추가 (담당자순/마감일순)"
```

---

## 자체 검토 (Spec Coverage)

| 스펙 요구사항 | 구현 Task |
|-------------|----------|
| DTO 필드명 `@SerializedName` 수정 | Task 1, 2 |
| TaskApi/NudgeApi URL 수정 | Task 3 |
| TeamApi getMembers → getTeamDetail | Task 3, 4 |
| UserApi 신규 추가 | Task 3, 4 |
| AuthApi logout 추가 | Task 3 |
| UpdateStatusRequest 신규 | Task 1 |
| TeamSettingsRequest 배열 형식 수정 | Task 1 |
| LoginViewModel 목업 제거 | Task 5 |
| RegisterViewModel 토큰 저장 버그 수정 | Task 5 |
| MainViewModel 목업 제거 | Task 7 |
| MainViewModel MOCK_MY_USER_ID 제거 | Task 7 |
| "내 할일" 별도 API 엔드포인트 사용 | Task 7 |
| CreateTaskViewModel 목업 제거 | Task 8 |
| TeamSettingsViewModel 목업 제거 + inviteCode | Task 9 |
| FCM 토큰 등록 (토큰 갱신 시) | Task 10 |
| FCM 토큰 등록 (로그인 시) | Task 10 |
| SortMode enum + 정렬 로직 | Task 6, 7 |
| 정렬 단위 테스트 | Task 6 |
| activity_main.xml 두 섹션 Spinner | Task 11 |
| MainActivity Spinner 이벤트 연결 | Task 11 |
