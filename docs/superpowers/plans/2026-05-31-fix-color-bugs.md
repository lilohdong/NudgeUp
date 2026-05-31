# fix/color-bugs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 유저 아바타 색상과 팀 색상이 실제 저장된 값으로 반영되도록 수정한다.

**Architecture:** 유저 색상 — `TaskListAdapter`/`MemberFilterAdapter`에서 `userId % 배열` 대신 `user.getColor()` hex 파싱으로 교체. 팀 색상 — 백엔드 `Team` 엔티티에 `color` 컬럼 추가, 팀 생성 API에 color 전달, `TeamTabAdapter`에서 서버 색상 사용.

**Tech Stack:** Android Java, Spring Boot 3, JPA (ddl-auto: update)

---

## File Map

**백엔드:**
| 파일 | 변경 유형 |
|---|---|
| `backend/.../team/entity/Team.java` | Modify |
| `backend/.../team/dto/request/CreateTeamRequest.java` | Modify |
| `backend/.../team/dto/response/TeamResponse.java` (backend) | Modify |
| `backend/.../team/service/TeamService.java` | Modify |

**앱:**
| 파일 | 변경 유형 |
|---|---|
| `app/.../data/dto/response/TeamResponse.java` | Modify |
| `app/.../data/dto/request/CreateTeamRequest.java` | Modify |
| `app/.../data/repository/TeamRepository.java` | Modify |
| `app/.../ui/team/CreateTeamViewModel.java` | Modify |
| `app/.../ui/team/CreateTeamActivity.java` | Modify |
| `app/.../ui/main/adapter/TeamTabAdapter.java` | Modify |
| `app/.../ui/main/adapter/TaskListAdapter.java` | Modify |
| `app/.../ui/main/adapter/MemberFilterAdapter.java` | Modify |

---

## Task 1: 브랜치 생성

- [ ] **Step 1**

```bash
git checkout main && git pull origin main
git checkout -b fix/color-bugs
```

---

## Task 2: 백엔드 — Team 엔티티 color 추가 + 팀 생성 API

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/entity/Team.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/dto/request/CreateTeamRequest.java`
- Modify 백엔드 `TeamResponse` (어디 있는지 확인 후 수정)
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/service/TeamService.java`

- [ ] **Step 1: Team.java에 color 컬럼 추가**

`Team.java`의 `inviteCode` 필드 아래에 추가:
```java
@Column(name = "color", length = 10)
private String color;
```

`@Builder` 생성자 수정:
```java
@Builder
public Team(String teamName, Long leaderId, String inviteCode, String color) {
    this.teamName = teamName;
    this.leaderId = leaderId;
    this.inviteCode = inviteCode;
    this.color = color != null ? color : "#7B3FE4"; // 기본값: purple
}
```

- [ ] **Step 2: 백엔드 CreateTeamRequest 파일 위치 확인 후 color 추가**

```bash
find B:/NudgeUp/backend -name "CreateTeamRequest.java" -o -name "CreateTeamCommand.java" | head -5
```

찾은 파일에 `color` 필드 추가:
```java
private String color;
public String getColor() { return color; }
```

- [ ] **Step 3: 백엔드 TeamResponse에 color 추가**

```bash
find B:/NudgeUp/backend -name "TeamResponse.java" | head -3
```

찾은 TeamResponse에 `color` 필드 추가 + `from()` 팩토리에 포함:
```java
private String color;
```
`from(Team team)` 메서드에서 `.color(team.getColor())` 포함.

- [ ] **Step 4: TeamService에서 팀 생성 시 color 전달**

`TeamService`에서 팀 생성 로직을 찾아 `color` 파라미터를 `Team.builder()`에 전달:
```java
.color(request.getColor())
```

- [ ] **Step 5: 컴파일 확인**

```bash
cd B:/NudgeUp/backend && ./gradlew compileJava 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add backend/
git commit -m "feat: Team 엔티티에 color 컬럼 추가, 팀 생성 API color 파라미터 추가"
```

---

## Task 3: 앱 — TeamResponse color 추가 + CreateTeam color 연동

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/data/dto/response/TeamResponse.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/dto/request/CreateTeamRequest.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/TeamRepository.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/team/CreateTeamViewModel.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/team/CreateTeamActivity.java`

- [ ] **Step 1: 앱 TeamResponse에 color 필드 추가**

`TeamResponse.java`에 추가:
```java
private String color;
public String getColor() { return color; }
```

- [ ] **Step 2: CreateTeamRequest에 color 추가**

`CreateTeamRequest.java`를 열어 현재 구조 확인 후 `color` 필드 추가. 생성자에도 포함:
```java
private String color;
public CreateTeamRequest(String teamName, String color) {
    this.teamName = teamName;
    this.color = color;
}
```
(기존 필드명 확인 필요 — `teamName`이 아닐 수 있음)

- [ ] **Step 3: TeamRepository.createTeam()에 color 파라미터 추가**

`TeamRepository.java`의 `createTeam(String name)` 메서드:
```java
public Call<ApiResponse<TeamDetailResponse>> createTeam(String name, String color) {
    return teamApi.createTeam(new CreateTeamRequest(name, color));
}
```
(`TeamApi.java`의 인터페이스도 동일하게 수정)

- [ ] **Step 4: CreateTeamViewModel.createTeam()에 color 추가**

```java
public void createTeam(String name, String colorHex) {
    teamRepository.createTeam(name, colorHex).enqueue(new Callback<ApiResponse<TeamDetailResponse>>() {
        @Override
        public void onResponse(Call<ApiResponse<TeamDetailResponse>> call,
                               Response<ApiResponse<TeamDetailResponse>> response) {
            createResult.setValue(response.isSuccessful()
                    && response.body() != null
                    && response.body().isSuccess());
        }
        @Override
        public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {
            networkError.setValue("네트워크 오류가 발생했습니다.");
            createResult.setValue(false);
        }
    });
}
```

- [ ] **Step 5: CreateTeamActivity에서 color 전달**

`CreateTeamActivity.java`의 `btnCreate.setOnClickListener` 안에서:
```java
// 기존
viewModel.createTeam(name);

// 변경 (selectedColor를 hex로 변환해 전달)
String colorHex = String.format("#%06X", 0xFFFFFF & selectedColor);
viewModel.createTeam(name, colorHex);
```

- [ ] **Step 6: 빌드 확인**

```bash
cd B:/NudgeUp/app && ./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/ \
        app/app/src/main/java/com/nudgeup/app/ui/team/
git commit -m "feat: 팀 생성 시 색상 서버에 저장, TeamResponse에 color 필드 추가"
```

---

## Task 4: 앱 — TeamTabAdapter, TaskListAdapter, MemberFilterAdapter 색상 수정

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/adapter/TeamTabAdapter.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/adapter/TaskListAdapter.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/adapter/MemberFilterAdapter.java`

색상 파싱 유틸 패턴 (각 어댑터에서 인라인 사용):
```java
// hex 문자열 → int 색상 (null 또는 파싱 실패 시 fallback)
private int parseColor(String colorHex, int fallback) {
    if (colorHex != null && !colorHex.isEmpty()) {
        try {
            return android.graphics.Color.parseColor(colorHex);
        } catch (Exception ignored) {}
    }
    return fallback;
}
```

- [ ] **Step 1: TeamTabAdapter에서 서버 color 사용**

현재:
```java
int color = TEAM_COLORS[i % TEAM_COLORS.length];
```

교체:
```java
int color = parseColor(team.getColor(), TEAM_COLORS[i % TEAM_COLORS.length]);
```

`addTabsTo()` 메서드 안에 `parseColor` 헬퍼 추가.

- [ ] **Step 2: TaskListAdapter에서 assignee color 사용**

현재 (`onBindViewHolder` 내):
```java
int avatarColor = AVATAR_COLORS[(int)(task.getAssignee().getId() % AVATAR_COLORS.length)];
holder.avatarBg.setColor(avatarColor);
```

교체:
```java
int fallback = AVATAR_COLORS[(int)(task.getAssignee().getId() % AVATAR_COLORS.length)];
int avatarColor = parseColor(task.getAssignee().getColor(), fallback);
holder.avatarBg.setColor(avatarColor);
```

`TaskListAdapter`에 `parseColor` 헬퍼 추가.

- [ ] **Step 3: MemberFilterAdapter에서 member color 사용**

`MemberFilterAdapter`의 `Item` 클래스에 color 필드 추가:
```java
private static class Item {
    long id;
    String name;
    String color;
    Item(long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }
}
```

`setMembers()` 수정:
```java
for (MemberResponse m : memberList) {
    items.add(new Item(m.getUserId(), m.getName(), m.getColor()));
}
```

`onBindViewHolder`에서 색상 결정 부분:
```java
// 기존
int colorIndex = item.id == -1 ? 0 : (int)(item.id % (AVATAR_COLORS.length - 1)) + 1;
int baseColor = AVATAR_COLORS[colorIndex];

// 교체
int fallbackIndex = item.id == -1 ? 0 : (int)(item.id % (AVATAR_COLORS.length - 1)) + 1;
int baseColor = item.id == -1
        ? AVATAR_COLORS[0]
        : parseColor(item.color, AVATAR_COLORS[fallbackIndex]);
```

`MemberFilterAdapter`에 `parseColor` 헬퍼 추가.

- [ ] **Step 4: 빌드 확인**

```bash
cd B:/NudgeUp/app && ./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/adapter/
git commit -m "fix: 어댑터에서 하드코딩 색상 대신 서버 저장 색상 사용"
```

---

## Task 5: PR 생성

- [ ] **Step 1**

```bash
git push -u origin fix/color-bugs
gh pr create --base main \
  --title "fix: 유저/팀 색상이 실제 저장값으로 반영되도록 수정" \
  --body "## Summary
- 백엔드: Team 엔티티에 color 컬럼 추가, 팀 생성 API color 파라미터 수신/저장
- 앱 CreateTeamActivity: selectedColor를 hex로 변환해 API 전달
- TeamTabAdapter: 서버 color 사용 (fallback: 인덱스 기반)
- TaskListAdapter: user.getColor() 사용 (fallback: 인덱스 기반)
- MemberFilterAdapter: member.getColor() 사용 (fallback: 인덱스 기반)

## Test plan
- [ ] 팀 생성 시 색상 선택 → 사이드바 팀 탭에 해당 색상 표시 확인
- [ ] 프로필에서 색상 변경 → 팀원 필터/작업 카드 아바타 색상 반영 확인
- [ ] color null인 경우 fallback 색상으로 표시되는지 확인"
```
