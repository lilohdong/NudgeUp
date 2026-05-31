# feat/team-join-profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 팀원도 팀 정보 화면 접근, 팀원 탈퇴/추방/팀장 탈퇴(위임 강제) 구현, + 버튼 개선(다크 테마), 프로필 역할 제거 + 회원탈퇴.

**Architecture:**
- 백엔드: `leaveTeam()` 팀장-혼자 케이스 처리(팀 삭제), `kickMember()` 추가, `deleteUser()` 팀장-위임 강제 체크 추가
- 앱: `TeamSettingsActivity` myRole 기반 UI 분기(팀장=설정+추방+탈퇴, 팀원=읽기전용+탈퇴), `MainActivity` + 버튼 다크 다이얼로그, `ProfileDialog` 회원탈퇴
- 모든 다이얼로그는 다크 테마 `bg_dialog` + `color_lime` 버튼 사용

**Tech Stack:** Android Java, Spring Boot 3

---

## File Map

**백엔드:**
| 파일 | 변경 |
|---|---|
| `backend/.../team/service/TeamService.java` | Modify: leaveTeam (혼자 케이스) + kickMember 추가 |
| `backend/.../team/controller/TeamController.java` | Modify: kickMember 엔드포인트 추가 |
| `backend/.../user/service/UserService.java` | Modify: deleteUser 추가 |
| `backend/.../user/controller/UserController.java` | Modify: DELETE /users/me 추가 |
| `backend/.../task/repository/TaskRepository.java` | Modify: clearAssigneeByUserId 추가 |
| `backend/.../nudge/repository/NudgeLogRepository.java` | Modify: deleteBySentById 추가 |
| `backend/.../team/repository/TeamMemberRepository.java` | Modify: deleteAllByUserId 추가 |
| `backend/.../global/exception/ErrorCode.java` | Modify: CANNOT_KICK_SELF 추가 |

**앱:**
| 파일 | 변경 |
|---|---|
| `app/.../res/layout/dialog_team_action.xml` | Create |
| `app/.../res/layout/dialog_invite_code.xml` | Create |
| `app/.../ui/main/MainActivity.java` | Modify: + 버튼, TeamSettings 버튼 모두 보이게 |
| `app/.../res/layout/activity_team_settings.xml` | Modify: 팀원 전용 섹션 추가, 탈퇴 버튼 추가 |
| `app/.../ui/team/TeamSettingsActivity.java` | Modify: 역할 분기 UI, 추방/탈퇴 로직 |
| `app/.../ui/team/TeamSettingsViewModel.java` | Modify: leaveTeam, kickMember 추가 |
| `app/.../data/api/TeamApi.java` | Modify: kickMember, leaveTeam 추가 |
| `app/.../data/repository/TeamRepository.java` (app) | Modify: kickMember, leaveTeam 추가 |
| `app/.../ui/main/dialog/ProfileDialog.java` | Modify: 역할 제거, 회원탈퇴 추가 |
| `app/.../res/layout/dialog_profile.xml` | Modify: tv_user_role 제거, btn_delete_account 추가 |
| `app/.../data/api/UserApi.java` | Modify: deleteMe 추가 |
| `app/.../data/repository/UserRepository.java` (app) | Modify: deleteMe 추가 |

---

## Task 1: 브랜치 생성

- [ ] **Step 1**

```bash
git checkout main && git pull origin main
git checkout -b feat/team-join-profile
```

---

## Task 2: 백엔드 — leaveTeam 팀장 혼자 케이스 + kickMember + ErrorCode

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/service/TeamService.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/controller/TeamController.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/global/exception/ErrorCode.java`

- [ ] **Step 1: ErrorCode에 CANNOT_KICK_SELF 추가**

`ErrorCode.java`의 `// Team` 섹션에:
```java
CANNOT_KICK_SELF(400, "자기 자신은 추방할 수 없습니다."),
```

- [ ] **Step 2: TeamService.leaveTeam() 수정**

현재 코드 (line ~154):
```java
@Transactional
public void leaveTeam(Long userId, Long teamId) {
    TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
    if (member.getRole() == TeamRole.LEADER) {
        throw new CustomException(ErrorCode.LEADER_CANNOT_LEAVE);
    }
    teamMemberRepository.delete(member);
}
```

교체:
```java
@Transactional
public void leaveTeam(Long userId, Long teamId) {
    TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
    if (member.getRole() == TeamRole.LEADER) {
        long memberCount = teamMemberRepository.countByTeamId(teamId);
        if (memberCount > 1) {
            // 다른 팀원이 있으면 위임 먼저
            throw new CustomException(ErrorCode.LEADER_CANNOT_LEAVE);
        }
        // 혼자인 팀장: 팀원 삭제 + 팀 해산
        teamMemberRepository.delete(member);
        teamRepository.deleteById(teamId);
        return;
    }
    teamMemberRepository.delete(member);
}
```

`TeamService`에서 `teamRepository`가 의존성으로 주입되어 있는지 확인. 없으면 필드에 추가:
```java
private final com.nudgeup.backend.domain.team.repository.TeamRepository teamRepository;
```

- [ ] **Step 3: TeamService.kickMember() 추가**

`leaveTeam()` 메서드 아래에 추가:
```java
@Transactional
public void kickMember(Long leaderId, Long teamId, Long targetUserId) {
    TeamMember leader = teamMemberRepository.findByTeamIdAndUserId(teamId, leaderId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
    if (leader.getRole() != TeamRole.LEADER) {
        throw new CustomException(ErrorCode.NOT_TEAM_LEADER);
    }
    if (leaderId.equals(targetUserId)) {
        throw new CustomException(ErrorCode.CANNOT_KICK_SELF);
    }
    TeamMember target = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
    teamMemberRepository.delete(target);
}
```

- [ ] **Step 4: TeamController에 kickMember 엔드포인트 추가**

`TeamController.java`의 `leaveTeam` 엔드포인트 아래에 추가:
```java
@DeleteMapping("/{teamId}/members/{memberId}")
public ResponseEntity<ApiResponse<Void>> kickMember(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long teamId,
        @PathVariable Long memberId) {
    Long userId = userDetails.getUser().getId();
    teamService.kickMember(userId, teamId, memberId);
    return ResponseEntity.ok(ApiResponse.success("팀원이 추방되었습니다.", null));
}
```

- [ ] **Step 5: 컴파일 확인**

```bash
cd B:/NudgeUp/backend && ./gradlew compileJava 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add backend/
git commit -m "feat: 팀장 혼자 탈퇴 시 팀 해산, 팀원 추방 API 추가"
```

---

## Task 3: 백엔드 — 회원탈퇴 API (팀장 체크 포함)

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/repository/TaskRepository.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/repository/TeamMemberRepository.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/user/service/UserService.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/user/controller/UserController.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/global/exception/ErrorCode.java`

- [ ] **Step 1: ErrorCode에 LEADER_HAS_MEMBERS 추가**

```java
LEADER_HAS_MEMBERS(400, "팀장을 위임한 후 탈퇴해주세요."),
```

- [ ] **Step 2: TaskRepository에 clearAssigneeByUserId 추가**

```java
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Modifying(clearAutomatically = true)
@Query("UPDATE Task t SET t.assigneeId = null WHERE t.assigneeId = :userId")
void clearAssigneeByUserId(@Param("userId") Long userId);
```

- [ ] **Step 3: NudgeLogRepository에 deleteBySentById 추가**

기존 메서드 아래에:
```java
@Modifying(clearAutomatically = true)
@Query("DELETE FROM NudgeLog n WHERE n.sentById = :userId")
void deleteBySentById(@Param("userId") Long userId);
```

- [ ] **Step 4: TeamMemberRepository에 deleteAllByUserId 추가**

```java
void deleteAllByUserId(Long userId);
```

- [ ] **Step 5: UserService에 deleteUser() 추가**

`UserService.java`에 의존성 추가 (필드):
```java
private final com.nudgeup.backend.domain.task.repository.TaskRepository taskRepository;
private final com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository nudgeLogRepository;
private final com.nudgeup.backend.domain.team.repository.TeamMemberRepository teamMemberRepository;
```

메서드 추가 (`updateFcmToken()` 아래):
```java
@Transactional
public void deleteUser(Long userId) {
    com.nudgeup.backend.domain.user.entity.User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    // 팀장이면 팀원 있는 팀이 있는지 체크
    List<com.nudgeup.backend.domain.team.entity.TeamMember> myMemberships =
            teamMemberRepository.findAllByUserId(userId);
    for (com.nudgeup.backend.domain.team.entity.TeamMember m : myMemberships) {
        if (m.getRole() == com.nudgeup.backend.domain.team.entity.TeamRole.LEADER) {
            long cnt = teamMemberRepository.countByTeamId(m.getTeamId());
            if (cnt > 1) {
                throw new CustomException(ErrorCode.LEADER_HAS_MEMBERS);
            }
        }
    }

    // 팀 멤버십 제거 (혼자인 팀은 팀도 삭제)
    for (com.nudgeup.backend.domain.team.entity.TeamMember m : myMemberships) {
        if (m.getRole() == com.nudgeup.backend.domain.team.entity.TeamRole.LEADER) {
            teamRepository.deleteById(m.getTeamId());
        }
    }
    teamMemberRepository.deleteAllByUserId(userId);

    // Task 담당자 해제
    taskRepository.clearAssigneeByUserId(userId);

    // 보낸 NudgeLog 삭제
    nudgeLogRepository.deleteBySentById(userId);

    // 유저 삭제
    userRepository.deleteById(userId);
}
```

`UserService`에 `teamRepository` 필드 필요:
```java
private final com.nudgeup.backend.domain.team.repository.TeamRepository teamRepository;
```

필요한 import 추가.

- [ ] **Step 6: UserController에 DELETE /me 추가**

```java
@DeleteMapping("/me")
public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    userService.deleteUser(userDetails.getUser().getId());
    return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다.", null));
}
```

- [ ] **Step 7: 컴파일 확인**

```bash
./gradlew compileJava 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: 커밋**

```bash
git add backend/
git commit -m "feat: 회원탈퇴 API (팀장 위임 체크, cascade 정리)"
```

---

## Task 4: 앱 — TeamApi, TeamRepository (app), TeamSettingsViewModel 확장

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/data/api/TeamApi.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/TeamRepository.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/team/TeamSettingsViewModel.java`

- [ ] **Step 1: TeamApi.java 읽기 후 leaveTeam, kickMember 추가**

먼저 파일을 읽어 현재 메서드 확인:
```bash
cat B:/NudgeUp/app/app/src/main/java/com/nudgeup/app/data/api/TeamApi.java
```

기존 메서드를 확인한 후 아래를 추가:
```java
import retrofit2.http.DELETE;
import retrofit2.http.Path;

@DELETE("teams/{teamId}/members/me")
Call<ApiResponse<Void>> leaveTeam(@Path("teamId") long teamId);

@DELETE("teams/{teamId}/members/{memberId}")
Call<ApiResponse<Void>> kickMember(
        @Path("teamId") long teamId,
        @Path("memberId") long memberId);
```

- [ ] **Step 2: TeamRepository (app)에 leaveTeam, kickMember 추가**

`TeamRepository.java`에 추가:
```java
public Call<ApiResponse<Void>> leaveTeam(long teamId) {
    return teamApi.leaveTeam(teamId);
}

public Call<ApiResponse<Void>> kickMember(long teamId, long memberId) {
    return teamApi.kickMember(teamId, memberId);
}
```

- [ ] **Step 3: TeamSettingsViewModel에 leaveTeam, kickMember 추가**

`TeamSettingsViewModel.java`를 읽어 현재 구조 파악 후 추가:
```java
private final MutableLiveData<Boolean> leaveResult = new MutableLiveData<>();
private final MutableLiveData<String> leaveError = new MutableLiveData<>();
public LiveData<Boolean> getLeaveResult() { return leaveResult; }
public LiveData<String> getLeaveError() { return leaveError; }

public void leaveTeam(long teamId) {
    teamRepository.leaveTeam(teamId).enqueue(new Callback<ApiResponse<Void>>() {
        @Override
        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
            if (response.isSuccessful()) {
                leaveResult.setValue(true);
            } else {
                // 서버 오류 메시지 파싱
                try {
                    if (response.errorBody() != null) {
                        String body = response.errorBody().string();
                        // "팀장을 위임한 후 탈퇴해주세요." 등 메시지 추출
                        org.json.JSONObject json = new org.json.JSONObject(body);
                        leaveError.setValue(json.optString("message", "탈퇴에 실패했습니다."));
                    }
                } catch (Exception e) {
                    leaveError.setValue("탈퇴에 실패했습니다.");
                }
            }
        }
        @Override
        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
            leaveError.setValue("네트워크 오류가 발생했습니다.");
        }
    });
}

public void kickMember(long teamId, long memberId) {
    teamRepository.kickMember(teamId, memberId).enqueue(new Callback<ApiResponse<Void>>() {
        @Override
        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
            if (response.isSuccessful()) {
                // 멤버 목록 새로고침
                loadMembers(teamId);
            }
        }
        @Override
        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
            networkError.setValue("네트워크 오류가 발생했습니다.");
        }
    });
}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd B:/NudgeUp/app && ./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/ \
        app/app/src/main/java/com/nudgeup/app/ui/team/TeamSettingsViewModel.java
git commit -m "feat: 앱 leaveTeam/kickMember API 연동"
```

---

## Task 5: 앱 레이아웃 — dialog_team_action, dialog_invite_code, activity_team_settings 수정

**Files:**
- Create: `app/app/src/main/res/layout/dialog_team_action.xml`
- Create: `app/app/src/main/res/layout/dialog_invite_code.xml`
- Modify: `app/app/src/main/res/layout/activity_team_settings.xml`

모든 다이얼로그는 `@drawable/bg_dialog` 배경 + `@color/color_lime` 강조 + `@color/text_primary`/`text_secondary` 텍스트 사용.

- [ ] **Step 1: dialog_team_action.xml 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_dialog"
    android:paddingStart="24dp"
    android:paddingEnd="24dp"
    android:paddingTop="20dp"
    android:paddingBottom="20dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="팀 참가"
        android:textSize="16sp"
        android:textStyle="bold"
        android:textColor="@color/text_primary"
        android:gravity="center"
        android:layout_marginBottom="20dp"/>

    <Button
        android:id="@+id/btn_create_team"
        android:layout_width="match_parent"
        android:layout_height="52dp"
        android:text="새 팀 만들기"
        android:textSize="14sp"
        android:textStyle="bold"
        android:textColor="@color/color_lime_text"
        android:background="@drawable/bg_btn_lime"
        android:layout_marginBottom="10dp"/>

    <Button
        android:id="@+id/btn_join_with_code"
        android:layout_width="match_parent"
        android:layout_height="52dp"
        android:text="초대 코드로 입장"
        android:textSize="14sp"
        android:textStyle="bold"
        android:textColor="@color/color_lime"
        android:background="@drawable/bg_btn_outline"/>

</LinearLayout>
```

- [ ] **Step 2: dialog_invite_code.xml 생성**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_dialog"
    android:paddingStart="24dp"
    android:paddingEnd="24dp"
    android:paddingTop="20dp"
    android:paddingBottom="20dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="초대 코드로 입장"
        android:textSize="16sp"
        android:textStyle="bold"
        android:textColor="@color/text_primary"
        android:gravity="center"
        android:layout_marginBottom="16dp"/>

    <EditText
        android:id="@+id/et_invite_code"
        android:layout_width="match_parent"
        android:layout_height="52dp"
        android:hint="초대 코드를 입력하세요"
        android:textColor="@color/text_primary"
        android:textColorHint="@color/text_secondary"
        android:textSize="14sp"
        android:background="@drawable/bg_input"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        android:singleLine="true"
        android:inputType="text"
        android:layout_marginBottom="20dp"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="end">

        <TextView
            android:id="@+id/btn_invite_cancel"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:gravity="center"
            android:paddingStart="20dp"
            android:paddingEnd="20dp"
            android:text="취소"
            android:textSize="14sp"
            android:textColor="@color/text_secondary"
            android:background="@android:color/transparent"/>

        <TextView
            android:id="@+id/btn_invite_confirm"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:gravity="center"
            android:paddingStart="20dp"
            android:paddingEnd="4dp"
            android:text="입장"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="@color/color_lime"
            android:background="@android:color/transparent"/>

    </LinearLayout>

</LinearLayout>
```

- [ ] **Step 3: activity_team_settings.xml에 팀원 전용 섹션 + 탈퇴 버튼 추가**

`activity_team_settings.xml`의 `ScrollView` 안 `LinearLayout` 끝 부분에 추가:

1. 팀원 전용 ReadOnly 멤버 목록 (팀장은 숨김):
```xml
<!-- 팀원 전용: 읽기 전용 멤버 목록 -->
<LinearLayout
    android:id="@+id/layout_member_readonly"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:visibility="gone">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="팀원 목록"
        android:textSize="11sp"
        android:textColor="@color/text_secondary"
        android:letterSpacing="0.06"
        android:layout_marginBottom="12dp"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_members_readonly"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:nestedScrollingEnabled="false"
        android:layout_marginBottom="24dp"/>

</LinearLayout>
```

2. 탈퇴 버튼 (팀장/팀원 모두):
```xml
<!-- 구분선 -->
<View
    android:layout_width="match_parent"
    android:layout_height="1dp"
    android:background="#2A2A45"
    android:layout_marginBottom="16dp"/>

<!-- 팀 탈퇴 버튼 -->
<Button
    android:id="@+id/btn_leave_team"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:text="팀 탈퇴"
    android:textColor="#FF4444"
    android:textSize="14sp"
    android:background="@drawable/bg_logout_btn"/>
```

제목 `TextView`의 `text="팀 설정"`도 코드에서 동적으로 변경할 수 있도록 `id` 추가:
```xml
android:id="@+id/tv_title"
android:text="팀 설정"
```

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/res/layout/
git commit -m "feat: 팀 액션/초대코드 다이얼로그 레이아웃 추가, 팀 설정 화면에 탈퇴 버튼 추가"
```

---

## Task 6: 앱 — MainActivity + 버튼 개선 + TeamSettings 모두에게 표시

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java`

- [ ] **Step 1: MainActivity.java 수정**

현재 코드들을 아래처럼 교체/추가.

**1) TeamSettings 버튼을 모두에게 표시**
```java
// 기존
btnTeamSettings.setVisibility(
        "LEADER".equals(team.getMyRole()) ? View.VISIBLE : View.GONE);

// 변경 — 항상 표시, myRole을 intent로 전달
btnTeamSettings.setVisibility(View.VISIBLE);
```

**2) TeamSettings intent에 myRole 추가**
```java
btnTeamSettings.setOnClickListener(v -> {
    if (viewModel.getCurrentTeam().getValue() == null) return;
    Intent intent = new Intent(this, TeamSettingsActivity.class);
    intent.putExtra("teamId", viewModel.getCurrentTeam().getValue().getId());
    intent.putExtra("myRole", viewModel.getCurrentTeam().getValue().getMyRole()); // 추가
    startActivity(intent);
});
```

**3) btnAddTeam 클릭 리스너 변경**
```java
// 기존
btnAddTeam.setOnClickListener(v ->
        createTeamLauncher.launch(new Intent(this, CreateTeamActivity.class)));

// 변경
btnAddTeam.setOnClickListener(v -> showTeamActionDialog());
```

**4) btnJoinTeamEmpty 클릭 리스너 변경**

기존 인라인 AlertDialog 전체 블록을 교체:
```java
btnJoinTeamEmpty.setOnClickListener(v -> showJoinTeamDialog());
```

**5) showTeamActionDialog() 메서드 추가**

```java
private void showTeamActionDialog() {
    android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_team_action, null);
    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create();
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    }
    dialogView.findViewById(R.id.btn_create_team).setOnClickListener(btn -> {
        dialog.dismiss();
        createTeamLauncher.launch(new Intent(this, CreateTeamActivity.class));
    });
    dialogView.findViewById(R.id.btn_join_with_code).setOnClickListener(btn -> {
        dialog.dismiss();
        showJoinTeamDialog();
    });
    dialog.show();
}
```

**6) showJoinTeamDialog() 메서드 추가**

```java
private void showJoinTeamDialog() {
    android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite_code, null);
    android.widget.EditText etCode = dialogView.findViewById(R.id.et_invite_code);
    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create();
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    }
    dialogView.findViewById(R.id.btn_invite_cancel).setOnClickListener(btn -> dialog.dismiss());
    dialogView.findViewById(R.id.btn_invite_confirm).setOnClickListener(btn -> {
        String code = etCode.getText().toString().trim();
        if (!code.isEmpty()) {
            dialogView.findViewById(R.id.btn_invite_confirm).setEnabled(false);
            viewModel.joinTeam(code);
            dialog.dismiss();
        }
    });
    dialog.show();
}
```

- [ ] **Step 2: 빌드 확인**

```bash
cd B:/NudgeUp/app && ./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java
git commit -m "feat: + 버튼 다크 테마 다이얼로그, TeamSettings 팀원도 접근 가능"
```

---

## Task 7: 앱 — TeamSettingsActivity 역할 분기 UI + 추방/탈퇴 로직

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/team/TeamSettingsActivity.java`

`TeamSettingsActivity.java`를 읽어 전체 구조를 파악한 후 아래 변경 사항을 적용한다.

- [ ] **Step 1: 파일 전체 읽기**

`TeamSettingsActivity.java` 전체를 읽어 현재 로직 파악.

- [ ] **Step 2: myRole 수신 및 역할 분기 추가**

`onCreate`에서 `teamId` 받는 줄 아래에 추가:
```java
String myRole = getIntent().getStringExtra("myRole");
boolean isLeader = "LEADER".equals(myRole);
```

**제목 변경** (tv_title id가 레이아웃에 있어야 함):
```java
TextView tvTitle = findViewById(R.id.tv_title);
if (tvTitle != null) tvTitle.setText(isLeader ? "팀 설정" : "팀 정보");
```

**팀장 전용 섹션 가시성**:
```java
// 기존 팀장 전용 뷰들을 layout_leader_settings 로 묶여 있다면:
View layoutLeaderSettings = findViewById(R.id.layout_leader_settings);
if (layoutLeaderSettings != null) {
    layoutLeaderSettings.setVisibility(isLeader ? View.VISIBLE : View.GONE);
}

// 팀원 전용 ReadOnly 멤버 목록
View layoutMemberReadonly = findViewById(R.id.layout_member_readonly);
if (layoutMemberReadonly != null) {
    layoutMemberReadonly.setVisibility(isLeader ? View.GONE : View.VISIBLE);
}
```

**참고**: `activity_team_settings.xml`에서 팀장 전용 뷰들(팀장 드롭다운, RecyclerView rvMembers, 저장 버튼)에 `android:id="@+id/layout_leader_settings"` 래퍼가 필요하면 레이아웃 파일도 함께 수정한다.

- [ ] **Step 3: 팀원 탈퇴 버튼 로직 추가**

`btnSave` 설정 코드 아래에 추가:
```java
Button btnLeaveTeam = findViewById(R.id.btn_leave_team);
btnLeaveTeam.setOnClickListener(v -> {
    android.view.View confirmView = getLayoutInflater().inflate(R.layout.dialog_delete_task, null);
    ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_title))
            .setText("팀 탈퇴");
    ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_message))
            .setText("정말 이 팀에서 탈퇴하시겠습니까?");
    ((android.widget.TextView) confirmView.findViewById(R.id.btn_dialog_confirm))
            .setText("탈퇴");

    android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(this)
            .setView(confirmView).create();
    if (confirmDialog.getWindow() != null) {
        confirmDialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    }
    confirmView.findViewById(R.id.btn_dialog_cancel)
            .setOnClickListener(btn -> confirmDialog.dismiss());
    confirmView.findViewById(R.id.btn_dialog_confirm)
            .setOnClickListener(btn -> {
                confirmDialog.dismiss();
                viewModel.leaveTeam(teamId);
            });
    confirmDialog.show();
});

viewModel.getLeaveResult().observe(this, success -> {
    if (Boolean.TRUE.equals(success)) {
        setResult(RESULT_OK);
        finish();
    }
});

viewModel.getLeaveError().observe(this, msg -> {
    if (msg != null) {
        com.nudgeup.app.util.ToastUtils.show(this, msg);
    }
});
```

- [ ] **Step 4: 팀원 추방 버튼 로직 (팀장 전용)**

`MemberNudgeAdapter`에 X 버튼 추가 또는 팀장 모드에서 각 아이템에 추방 버튼 표시.

먼저 `MemberNudgeAdapter.java` 읽기:
```bash
cat B:/NudgeUp/app/app/src/main/java/com/nudgeup/app/ui/team/MemberNudgeAdapter.java
```

어댑터에 `OnKickListener` 인터페이스와 `btnKick` 뷰를 추가:
```java
public interface OnKickListener {
    void onKick(long memberId, String memberName);
}
```

생성자에 `OnKickListener kickListener` 추가 (nullable), 팀장 모드일 때만 X 버튼 표시.

`TeamSettingsActivity`에서 어댑터 생성 시:
```java
memberNudgeAdapter = new MemberNudgeAdapter(members,
        (userId, canNudge) -> viewModel.setNudgePermission(userId, canNudge),
        isLeader ? (memberId, memberName) -> {
            // 추방 확인 다이얼로그
            android.view.View confirmView = getLayoutInflater()
                    .inflate(R.layout.dialog_delete_task, null);
            ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_title))
                    .setText("팀원 추방");
            ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_message))
                    .setText(memberName + "님을 팀에서 추방하시겠습니까?");
            ((android.widget.TextView) confirmView.findViewById(R.id.btn_dialog_confirm))
                    .setText("추방");
            android.app.AlertDialog d = new android.app.AlertDialog.Builder(this)
                    .setView(confirmView).create();
            if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            confirmView.findViewById(R.id.btn_dialog_cancel)
                    .setOnClickListener(btn -> d.dismiss());
            confirmView.findViewById(R.id.btn_dialog_confirm)
                    .setOnClickListener(btn -> {
                        d.dismiss();
                        viewModel.kickMember(teamId, memberId);
                    });
            d.show();
        } : null);
```

- [ ] **Step 5: 빌드 확인**

```bash
./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/team/
git commit -m "feat: TeamSettingsActivity 역할별 UI 분기, 탈퇴/추방 기능 추가"
```

---

## Task 8: 앱 — ProfileDialog 역할 제거 + 회원탈퇴

**Files:**
- Modify: `app/app/src/main/res/layout/dialog_profile.xml`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/dialog/ProfileDialog.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/api/UserApi.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/UserRepository.java` (app)

- [ ] **Step 1: dialog_profile.xml — tv_user_role 제거 + btn_delete_account 추가**

`dialog_profile.xml`에서 아래 블록 전체 삭제:
```xml
<!-- 역할 배지 (항상 표시) -->
<TextView
    android:id="@+id/tv_user_role"
    .../>
```

`layout_view_mode` 안의 `btn_logout` 아래에 추가:
```xml
<View
    android:layout_width="match_parent"
    android:layout_height="1dp"
    android:background="#2A2A45"
    android:layout_marginTop="10dp"
    android:layout_marginBottom="10dp"/>

<Button
    android:id="@+id/btn_delete_account"
    android:layout_width="match_parent"
    android:layout_height="40dp"
    android:text="회원탈퇴"
    android:textColor="#888899"
    android:textSize="13sp"
    android:background="@android:color/transparent"/>
```

- [ ] **Step 2: ProfileDialog.java — ARG_IS_LEADER 제거 + newInstance() 수정**

아래 항목들 삭제:
1. `private static final String ARG_IS_LEADER = "is_leader";`
2. `newInstance(boolean isLeader)` → `newInstance()` (파라미터 제거)
3. `onCreateView`의 아래 2줄 삭제:
   ```java
   boolean isLeader = getArguments() != null && getArguments().getBoolean(ARG_IS_LEADER, false);
   ((TextView) view.findViewById(R.id.tv_user_role)).setText(isLeader ? "팀장" : "팀원");
   ```

`newInstance` 변경:
```java
public static ProfileDialog newInstance() {
    return new ProfileDialog();
}
```

- [ ] **Step 3: UserApi.java에 DELETE /users/me 추가**

```java
import retrofit2.http.DELETE;

@DELETE("users/me")
Call<ApiResponse<Void>> deleteMe();
```

- [ ] **Step 4: UserRepository (app)에 deleteMe() 추가**

```java
public Call<ApiResponse<Void>> deleteMe() {
    return userApi.deleteMe();
}
```

- [ ] **Step 5: ProfileDialog에 회원탈퇴 버튼 로직 추가**

`onCreateView`의 로그아웃 버튼 아래에 추가:
```java
Button btnDeleteAccount = view.findViewById(R.id.btn_delete_account);
btnDeleteAccount.setOnClickListener(v -> {
    android.view.View confirmView = android.view.LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_task, null);
    ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_title))
            .setText("회원탈퇴");
    ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_message))
            .setText("정말 탈퇴하시겠습니까?\n팀 멤버십과 작업 정보가 초기화됩니다.\n(팀장이라면 팀장 위임 후 탈퇴 가능)");
    ((android.widget.TextView) confirmView.findViewById(R.id.btn_dialog_confirm))
            .setText("탈퇴");

    android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(requireContext())
            .setView(confirmView).create();
    if (confirmDialog.getWindow() != null) {
        confirmDialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    confirmView.findViewById(R.id.btn_dialog_cancel)
            .setOnClickListener(btn -> confirmDialog.dismiss());
    confirmView.findViewById(R.id.btn_dialog_confirm)
            .setOnClickListener(btn -> {
                confirmDialog.dismiss();
                userRepo.deleteMe().enqueue(new retrofit2.Callback<com.nudgeup.app.data.dto.response.ApiResponse<Void>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<Void>> call,
                            retrofit2.Response<com.nudgeup.app.data.dto.response.ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            NudgeUpApplication.getTokenManager().clearTokens();
                            android.content.Intent intent = new android.content.Intent(
                                    requireActivity(), com.nudgeup.app.ui.auth.LoginActivity.class);
                            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            dismiss();
                        } else {
                            try {
                                if (response.errorBody() != null) {
                                    org.json.JSONObject json = new org.json.JSONObject(
                                            response.errorBody().string());
                                    ToastUtils.show(requireContext(),
                                            json.optString("message", "탈퇴에 실패했습니다."));
                                }
                            } catch (Exception e) {
                                ToastUtils.show(requireContext(), "탈퇴에 실패했습니다.");
                            }
                        }
                    }
                    @Override
                    public void onFailure(
                            retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<Void>> call,
                            Throwable t) {
                        ToastUtils.show(requireContext(), "네트워크 오류가 발생했습니다.");
                    }
                });
            });
    confirmDialog.show();
});
```

- [ ] **Step 6: MainActivity.java ProfileDialog 호출 수정**

```java
// 기존
boolean isLeader = viewModel.isCurrentUserLeader();
ProfileDialog.newInstance(isLeader).show(getSupportFragmentManager(), "profile");

// 변경
ProfileDialog.newInstance().show(getSupportFragmentManager(), "profile");
```

`isLeader` 로컬 변수가 더 이상 필요 없으면 제거. `btnMy.setOnClickListener`:
```java
btnMy.setOnClickListener(v ->
        ProfileDialog.newInstance().show(getSupportFragmentManager(), "profile"));
```

- [ ] **Step 7: 빌드 확인**

```bash
./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/dialog/ProfileDialog.java \
        app/app/src/main/res/layout/dialog_profile.xml \
        app/app/src/main/java/com/nudgeup/app/data/ \
        app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java
git commit -m "feat: ProfileDialog 역할 배지 제거, 회원탈퇴 기능 추가"
```

---

## Task 9: 전체 빌드 + PR 생성

- [ ] **Step 1: 백엔드 전체 컴파일**

```bash
cd B:/NudgeUp/backend && ./gradlew compileJava 2>&1 | tail -3
```

- [ ] **Step 2: 앱 전체 빌드**

```bash
cd B:/NudgeUp/app && ./gradlew assembleDebug 2>&1 | tail -3
```

- [ ] **Step 3: push + PR**

```bash
git push -u origin feat/team-join-profile
gh pr create --base main \
  --title "feat: 팀 참가 UI / 팀 정보 화면 / 탈퇴-추방 / 회원탈퇴" \
  --body "## Summary
- + 버튼: 팀 생성 or 초대코드 다크 테마 선택 다이얼로그
- 초대코드 입력 UI 다크 테마 통일 (팀 있을 때도 사용 가능)
- TeamSettingsActivity: 팀원도 접근 — 팀원=팀 정보+탈퇴, 팀장=설정+추방+탈퇴
- 팀 탈퇴: 팀장+팀원 있으면 '위임 먼저' 에러, 팀장 혼자면 팀 해산
- 팀원 추방: 팀장이 각 팀원 X 버튼 → 확인 다이얼로그 → API
- ProfileDialog: 팀장/팀원 역할 배지 제거
- 회원탈퇴: 확인 다이얼로그 → API → 토큰 삭제 → 로그인 화면
- 백엔드 DELETE /api/v1/teams/{teamId}/members/{memberId}, DELETE /api/v1/users/me

## Test plan
- [ ] + 버튼 → 다크 다이얼로그 (팀 만들기/초대코드 입장)
- [ ] 초대코드 입력 → 팀 합류
- [ ] 팀원: TeamSettings → '팀 정보' 제목, 읽기 전용, 탈퇴 버튼
- [ ] 팀장: TeamSettings → 팀원 추방(X) 확인 다이얼로그
- [ ] 팀장(팀원 있음) 탈퇴 → '팀장 위임 먼저' 메시지
- [ ] 팀장(혼자) 탈퇴 → 팀 해산
- [ ] MY → 역할 배지 없는지 확인
- [ ] MY → 회원탈퇴 클릭 → 확인 → 탈퇴 → 로그인 화면"
```
