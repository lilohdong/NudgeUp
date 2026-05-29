# API 연결 및 작업 목록 정렬 기능 설계

**날짜**: 2026-05-29  
**브랜치**: feature/api-connect  
**대상**: Android 앱 전체 ViewModel 목업 제거 + 실제 백엔드 API 연결 + 작업 정렬 기능 추가

---

## 1. 범위

| 레이어 | 작업 내용 |
|--------|----------|
| Layer A — 데이터 레이어 | DTO 필드명 수정, API 인터페이스 URL 수정, 누락 API/DTO 추가 |
| Layer B — ViewModel | 전체 목업 블록 제거, 실제 API 호출로 교체, FCM 토큰 등록 |
| Layer C — UI | 메인 화면 두 섹션(팀원 전체 할일 / 내 할일)에 정렬 드롭박스 추가 |

---

## 2. 발견된 불일치 및 수정 사항

### 2-1. API URL 오류

| 파일 | 현재 (잘못됨) | 수정 후 |
|------|--------------|---------|
| `TaskApi` | `GET tasks?teamId=` | `GET teams/{teamId}/tasks` |
| `TaskApi` | `POST tasks` | `POST teams/{teamId}/tasks` |
| `TaskApi` | `PATCH tasks/{taskId}/status` | `PATCH teams/{teamId}/tasks/{taskId}/status` |
| `NudgeApi` | `POST nudges/{taskId}` | `POST teams/{teamId}/tasks/{taskId}/nudge` |
| `TeamApi` | `GET teams/{teamId}/members` | `GET teams/{teamId}` (팀 상세에 members 포함) |

### 2-2. DTO 필드명 불일치 (Gson 역직렬화 실패 원인)

`@SerializedName` 어노테이션으로 수정:

| DTO 파일 | 현재 필드명 | API 명세 필드명 |
|---------|------------|----------------|
| `RegisterRequest` | `name` | `username` |
| `TeamResponse` | `id`, `name` | `teamId`, `teamName` |
| `TaskResponse` | `id` | `taskId` |
| `MemberResponse` | `name` | `username` |
| `User` (domain) | `id`, `name` | `userId`, `username` |
| `TeamSettingsRequest` | `nudgePermissions: Map<Long,Boolean>` | `memberPermissions: [{userId, canNudge}]` |

### 2-3. 신규 추가 항목

- `UpdateStatusRequest` DTO — `{ "status": "IN_PROGRESS" }` 형태 (현재 `@Body String` 동작 안 함)
- `MemberPermissionRequest` DTO — `TeamSettingsRequest` 내부 배열 항목
- `UserApi` 인터페이스 — `GET /users/me`, `PATCH /users/me`, `PUT /users/me/fcm-token`
- `UserRepository` 클래스
- `AuthApi`에 `POST /auth/logout` 추가
- `CreateTaskRequest`에서 `teamId` 필드 제거 (path param으로 이동)
- `TaskApi`에 `GET teams/{teamId}/tasks/my` 추가 (내 작업 전용 엔드포인트)

---

## 3. Layer A — 데이터 레이어 상세 설계

### TaskApi (전면 재작성)

```java
@GET("teams/{teamId}/tasks")
Call<ApiResponse<List<TaskResponse>>> getTasks(@Path("teamId") long teamId);

@GET("teams/{teamId}/tasks/my")
Call<ApiResponse<List<TaskResponse>>> getMyTasks(@Path("teamId") long teamId);

@POST("teams/{teamId}/tasks")
Call<ApiResponse<TaskResponse>> createTask(
    @Path("teamId") long teamId,
    @Body CreateTaskRequest request
);

@PATCH("teams/{teamId}/tasks/{taskId}/status")
Call<ApiResponse<TaskResponse>> updateStatus(
    @Path("teamId") long teamId,
    @Path("taskId") long taskId,
    @Body UpdateStatusRequest request
);
```

### NudgeApi (URL 수정)

```java
@POST("teams/{teamId}/tasks/{taskId}/nudge")
Call<ApiResponse<Void>> sendNudge(
    @Path("teamId") long teamId,
    @Path("taskId") long taskId
);
```

### TeamApi (members 엔드포인트 변경)

```java
@GET("teams/{teamId}")   // 팀 상세 (members 포함)
Call<ApiResponse<TeamDetailResponse>> getTeamDetail(@Path("teamId") long teamId);
```

`TeamDetailResponse` — 기존 `TeamResponse`에 `List<MemberResponse> members` 필드 추가.

### UserApi (신규)

```java
@GET("users/me")
Call<ApiResponse<UserResponse>> getMe();

@PUT("users/me/fcm-token")
Call<ApiResponse<Void>> registerFcmToken(@Body FcmTokenRequest request);
```

---

## 4. Layer B — ViewModel 목업 제거 상세

### LoginViewModel
- 목업 블록(`loginResult.setValue(UiResult.success())`) 제거
- 주석 해제하여 실제 `authRepository.login()` 호출

### MainViewModel
- `loadTeams()` → `teamRepository.getMyTeams()` 실제 호출
- `loadTasks(teamId)` → `taskRepository.getTasks(teamId)` 실제 호출
- `loadMyTasks(teamId)` → `taskRepository.getMyTasks(teamId)` 실제 호출 (현재 없는 메서드, 신규 추가)
- `loadMembers(teamId)` → `teamRepository.getTeamDetail(teamId)` 호출 후 members 추출
- `sendNudge(teamId, taskId)` → `nudgeRepository.sendNudge(teamId, taskId)` 실제 호출
- `updateTaskStatus(teamId, taskId, status)` → `taskRepository.updateStatus()` 실제 호출
- `MOCK_MY_USER_ID` 제거 → `TokenManager.getUserId()` 사용
- "내 할일" 분리: 별도 API 호출(`getMyTasks`)로 서버에서 직접 받음

### CreateTaskViewModel
- `loadTeamMembers(teamId)` → `teamRepository.getTeamDetail(teamId)` 호출
- `createTask()` → 주석 해제, 실제 `taskRepository.createTask()` 호출

### TeamSettingsViewModel
- `loadMembers(teamId)` → `teamRepository.getTeamDetail(teamId)` 호출
- `inviteCode` → teamDetail의 `inviteCode` 필드에서 추출
- `saveSettings()` → 주석 해제, 실제 `teamRepository.updateSettings()` 호출

### FCM 토큰 등록
- `NudgeFcmService.onNewToken()` 에서 로그인 상태면 `userRepository.registerFcmToken()` 호출
- 로그인 성공 직후 현재 FCM 토큰 조회 후 서버 등록

---

## 5. Layer C — 정렬 기능 상세 설계

### 정렬 옵션 (SortMode enum)

```java
public enum SortMode {
    DEFAULT,    // 마감 임박 → 진행 중 → 대기 → 완료 (기존 로직)
    BY_PERSON,  // 담당자 이름 가나다순
    BY_DEADLINE // 마감일 빠른 순 (null은 맨 뒤)
}
```

### UI 변경

`activity_main.xml` 에서 두 섹션 헤더 각각에 `Spinner` 추가:

```
[팀원 전체 할일]  [정렬 ▼]   ← Spinner (teamTasksSortSpinner)
...RecyclerView...

[내 할일]         [정렬 ▼]   ← Spinner (myTasksSortSpinner)  
...RecyclerView...
```

각 Spinner는 독립적으로 동작 (서로 다른 정렬 선택 가능).

### ViewModel 변경

```java
private SortMode teamTasksSortMode = SortMode.DEFAULT;
private SortMode myTasksSortMode = SortMode.DEFAULT;

public void setTeamTasksSortMode(SortMode mode) {
    teamTasksSortMode = mode;
    applyFilter(); // 재정렬 트리거
}

public void setMyTasksSortMode(SortMode mode) {
    myTasksSortMode = mode;
    applyMyTasksFilter();
}
```

`sortTasks()` 메서드를 `SortMode` 파라미터를 받도록 확장:

```java
private List<TaskResponse> sortTasks(List<TaskResponse> list, SortMode mode) {
    switch (mode) {
        case BY_PERSON:   // assignee.username 가나다순
        case BY_DEADLINE: // dueDate 오름차순, null은 맨 뒤
        default:          // 기존 priority 로직
    }
}
```

---

## 6. 구현 순서

1. **DTO 수정** — `@SerializedName` 추가, `TeamSettingsRequest` 재작성, `UpdateStatusRequest` 신규
2. **API 인터페이스 수정** — `TaskApi`, `NudgeApi`, `TeamApi`, `UserApi` 신규
3. **Repository 수정** — `TaskRepository`, `TeamRepository`, `NudgeRepository`, `UserRepository` 신규
4. **LoginViewModel** — 목업 제거
5. **MainViewModel** — 목업 제거 + 정렬 기능
6. **CreateTaskViewModel** — 목업 제거
7. **TeamSettingsViewModel** — 목업 제거
8. **FCM 토큰 등록** — NudgeFcmService + 로그인 시 등록
9. **activity_main.xml** — 정렬 Spinner 추가
10. **MainActivity** — Spinner 이벤트 → ViewModel 연결

---

## 7. 에러 처리 방침

- 모든 API 실패는 `onFailure` / 비성공 응답에서 `UiResult.fail(message)` 또는 LiveData에 `false` emit
- 401 응답 시: 토큰 만료 → 로그인 화면으로 이동 (현재 `AuthInterceptor`에서 처리 예정)
- 네트워크 오류는 사용자에게 Toast 또는 기존 `UiResult.fail()` 패턴으로 표시
