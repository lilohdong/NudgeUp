# feat/team-join-profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 팀이 있을 때도 초대코드 입장이 가능하도록 + 버튼 개선, 초대코드 UI 다크 테마 통일, ProfileDialog 역할 배지 제거 + 회원탈퇴 기능 추가.

**Architecture:** 백엔드에 `DELETE /api/v1/users/me` 추가 (TeamMember, NudgeLog, Task 연관 정리 후 User 삭제). 앱은 + 버튼 클릭 시 "팀 만들기 / 초대코드 입장" 선택 다이얼로그 표시; 초대코드 입력 UI를 재사용 가능한 `showJoinTeamDialog()` 메서드로 추출해 다크 테마 적용.

**Tech Stack:** Android Java, Spring Boot 3, JPA

---

## File Map

**백엔드:**
| 파일 | 변경 유형 |
|---|---|
| `backend/.../task/repository/TaskRepository.java` | Modify |
| `backend/.../nudge/repository/NudgeLogRepository.java` | Modify |
| `backend/.../team/repository/TeamMemberRepository.java` | Modify |
| `backend/.../user/service/UserService.java` | Modify |
| `backend/.../user/controller/UserController.java` | Modify |

**앱:**
| 파일 | 변경 유형 |
|---|---|
| `app/.../res/layout/dialog_team_action.xml` | Create |
| `app/.../res/layout/dialog_invite_code.xml` | Create |
| `app/.../ui/main/MainActivity.java` | Modify |
| `app/.../ui/main/dialog/ProfileDialog.java` | Modify |
| `app/.../res/layout/dialog_profile.xml` | Modify |
| `app/.../data/api/UserApi.java` | Modify |
| `app/.../data/repository/UserRepository.java` | Modify |

---

## Task 1: 브랜치 생성

- [ ] **Step 1: 브랜치 생성**

```bash
git checkout main && git pull origin main
git checkout -b feat/team-join-profile
```

---

## Task 2: 백엔드 — 회원탈퇴 cascade 쿼리 추가

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/task/repository/TaskRepository.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/repository/TeamMemberRepository.java`

- [ ] **Step 1: TaskRepository에 assigneeId/createdById null 처리 쿼리 추가**

`TaskRepository.java` 기존 메서드 목록 아래에 추가:
```java
@Modifying(clearAutomatically = true)
@Query("UPDATE Task t SET t.assigneeId = null WHERE t.assigneeId = :userId")
void clearAssigneeByUserId(@Param("userId") Long userId);
// createdById는 NOT NULL 컬럼이므로 null 처리 안 함 — 탈퇴한 유저의 작업은 담당자 없음 표시만
```

import 추가가 필요하면:
```java
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

- [ ] **Step 2: NudgeLogRepository에 보낸 독촉 삭제 쿼리 추가**

`NudgeLogRepository.java`에 추가 (기존 deleteByTaskIdIn 아래):
```java
@Modifying(clearAutomatically = true)
@Query("DELETE FROM NudgeLog n WHERE n.sentById = :userId")
void deleteBySentById(@Param("userId") Long userId);
```

- [ ] **Step 3: TeamMemberRepository에 유저 전체 멤버십 삭제 추가**

`TeamMemberRepository.java`에 추가:
```java
void deleteAllByUserId(Long userId);
```

- [ ] **Step 4: 컴파일 확인**

```bash
cd B:/NudgeUp/backend
./gradlew compileJava 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/task/repository/TaskRepository.java \
        backend/src/main/java/com/nudgeup/backend/domain/nudge/repository/NudgeLogRepository.java \
        backend/src/main/java/com/nudgeup/backend/domain/team/repository/TeamMemberRepository.java
git commit -m "feat: 회원탈퇴 cascade용 Repository 쿼리 추가"
```

---

## Task 3: 백엔드 — UserService.deleteUser() + UserController DELETE 엔드포인트

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/user/service/UserService.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/user/controller/UserController.java`

- [ ] **Step 1: UserService에 의존성 추가 및 deleteUser() 구현**

`UserService.java`의 필드 목록 상단에 아래 3개 필드 추가:
```java
private final com.nudgeup.backend.domain.task.repository.TaskRepository taskRepository;
private final com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository nudgeLogRepository;
private final com.nudgeup.backend.domain.team.repository.TeamMemberRepository teamMemberRepository;
```

`updateFcmToken()` 아래에 `deleteUser()` 추가:
```java
@Transactional
public void deleteUser(Long userId) {
    userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    // 1. 팀 멤버십 제거
    teamMemberRepository.deleteAllByUserId(userId);

    // 2. 할당된 Task 담당자 연결 해제 (createdById는 NOT NULL이라 그대로 유지)
    taskRepository.clearAssigneeByUserId(userId);

    // 3. 보낸 NudgeLog 삭제
    nudgeLogRepository.deleteBySentById(userId);

    // 4. 유저 삭제
    userRepository.deleteById(userId);
}
```

import 추가:
```java
import com.nudgeup.backend.domain.task.repository.TaskRepository;
import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.team.repository.TeamMemberRepository;
```

- [ ] **Step 2: UserController에 DELETE /me 엔드포인트 추가**

`UserController.java`의 `updateFcmToken()` 아래에 추가:
```java
@DeleteMapping("/me")
public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUser().getId();
    userService.deleteUser(userId);
    return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다.", null));
}
```

- [ ] **Step 3: 전체 컴파일 확인**

```bash
./gradlew compileJava 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/user/service/UserService.java \
        backend/src/main/java/com/nudgeup/backend/domain/user/controller/UserController.java
git commit -m "feat: 회원탈퇴 API 추가 (DELETE /api/v1/users/me)"
```

---

## Task 4: 앱 — UserApi, UserRepository에 회원탈퇴 추가

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/data/api/UserApi.java`
- Modify: `app/app/src/main/java/com/nudgeup/app/data/repository/UserRepository.java`

- [ ] **Step 1: UserApi.java에 DELETE 추가**

기존 파일에 import와 메서드 추가:
```java
import retrofit2.http.DELETE;

@DELETE("users/me")
Call<ApiResponse<Void>> deleteMe();
```

- [ ] **Step 2: UserRepository.java에 deleteMe() 추가**

```java
public Call<ApiResponse<Void>> deleteMe() {
    return userApi.deleteMe();
}
```

- [ ] **Step 3: 빌드 확인**

```bash
cd B:/NudgeUp/app
./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/data/api/UserApi.java \
        app/app/src/main/java/com/nudgeup/app/data/repository/UserRepository.java
git commit -m "feat: 앱 UserApi/UserRepository에 deleteMe() 추가"
```

---

## Task 5: 앱 — dialog_team_action.xml 생성 (팀 만들기 / 초대코드)

**Files:**
- Create: `app/app/src/main/res/layout/dialog_team_action.xml`

"+" 버튼 클릭 시 보여줄 다크 테마 선택 다이얼로그.

- [ ] **Step 1: 파일 생성**

`app/app/src/main/res/layout/dialog_team_action.xml`:

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

- [ ] **Step 2: 커밋**

```bash
git add app/app/src/main/res/layout/dialog_team_action.xml
git commit -m "feat: 팀 생성/초대코드 선택 다이얼로그 레이아웃 추가"
```

---

## Task 6: 앱 — dialog_invite_code.xml 생성 (다크 테마 초대코드 입력)

**Files:**
- Create: `app/app/src/main/res/layout/dialog_invite_code.xml`

- [ ] **Step 1: 파일 생성**

`app/app/src/main/res/layout/dialog_invite_code.xml`:

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

- [ ] **Step 2: 커밋**

```bash
git add app/app/src/main/res/layout/dialog_invite_code.xml
git commit -m "feat: 다크 테마 초대코드 입력 다이얼로그 레이아웃 추가"
```

---

## Task 7: 앱 — MainActivity + 버튼 개선 + showJoinTeamDialog() 추출

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java`

현재:
- `btnAddTeam` → `createTeamLauncher.launch(CreateTeamActivity)` 로만 연결
- `btnJoinTeamEmpty` → 인라인 AlertDialog (라이트 테마)

변경 후:
- `btnAddTeam` → `dialog_team_action.xml` 다이얼로그 표시 (팀 만들기 / 초대코드)
- `btnJoinTeamEmpty` → `showJoinTeamDialog()` 호출
- 초대코드 입력은 `showJoinTeamDialog()` 한 곳에서만 처리

- [ ] **Step 1: btnAddTeam 클릭 리스너 변경**

기존:
```java
btnAddTeam.setOnClickListener(v ->
        createTeamLauncher.launch(new Intent(this, CreateTeamActivity.class)));
```

교체:
```java
btnAddTeam.setOnClickListener(v -> showTeamActionDialog());
```

- [ ] **Step 2: btnJoinTeamEmpty 클릭 리스너 변경**

기존 `btnJoinTeamEmpty.setOnClickListener` 전체 블록(AlertDialog 생성 포함)을 아래로 교체:
```java
btnJoinTeamEmpty.setOnClickListener(v -> showJoinTeamDialog());
```

- [ ] **Step 3: showTeamActionDialog() 메서드 추가**

`MainActivity.java`에 추가:
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

- [ ] **Step 4: showJoinTeamDialog() 메서드 추가**

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

- [ ] **Step 5: 빌드 확인**

```bash
cd B:/NudgeUp/app
./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java
git commit -m "feat: + 버튼에 팀 생성/초대코드 입장 선택 다이얼로그 추가"
```

---

## Task 8: 앱 — dialog_profile.xml 역할 배지 제거 + 회원탈퇴 버튼 추가

**Files:**
- Modify: `app/app/src/main/res/layout/dialog_profile.xml`

- [ ] **Step 1: tv_user_role 텍스트뷰 제거**

`dialog_profile.xml`에서 아래 전체 블록을 삭제:
```xml
<!-- 역할 배지 (항상 표시) -->
<TextView
    android:id="@+id/tv_user_role"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:paddingStart="12dp"
    android:paddingEnd="12dp"
    android:paddingTop="4dp"
    android:paddingBottom="4dp"
    android:textSize="11sp"
    android:textColor="@color/color_lime"
    android:background="@drawable/bg_role_badge"/>
```

- [ ] **Step 2: layout_view_mode에 회원탈퇴 버튼 추가**

`layout_view_mode` 안의 `btn_logout` 아래에 추가:

```xml
<!-- 구분선 -->
<View
    android:layout_width="match_parent"
    android:layout_height="1dp"
    android:background="#2A2A45"
    android:layout_marginTop="10dp"
    android:layout_marginBottom="10dp"/>

<!-- 회원탈퇴 버튼 -->
<Button
    android:id="@+id/btn_delete_account"
    android:layout_width="match_parent"
    android:layout_height="40dp"
    android:text="회원탈퇴"
    android:textColor="#888899"
    android:textSize="13sp"
    android:background="@android:color/transparent"/>
```

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/res/layout/dialog_profile.xml
git commit -m "feat: ProfileDialog 역할 배지 제거 + 회원탈퇴 버튼 레이아웃 추가"
```

---

## Task 9: 앱 — ProfileDialog.java 역할 코드 제거 + 회원탈퇴 로직 추가

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/main/dialog/ProfileDialog.java`

- [ ] **Step 1: ARG_IS_LEADER 관련 코드 전부 제거**

아래 항목들을 삭제:
1. `private static final String ARG_IS_LEADER = "is_leader";` 상수
2. `newInstance(boolean isLeader)` 팩토리 메서드에서 `args.putBoolean(ARG_IS_LEADER, isLeader)` 줄 삭제
3. `onCreateView`의 아래 2줄 삭제:
   ```java
   boolean isLeader = getArguments() != null && getArguments().getBoolean(ARG_IS_LEADER, false);
   ((TextView) view.findViewById(R.id.tv_user_role)).setText(isLeader ? "팀장" : "팀원");
   ```

`newInstance`는 파라미터 없는 버전으로 변경:
```java
public static ProfileDialog newInstance() {
    return new ProfileDialog();
}
```

- [ ] **Step 2: MainActivity.java에서 ProfileDialog 호출 수정**

`MainActivity.java`에서:
```java
// 기존
ProfileDialog.newInstance(isLeader).show(getSupportFragmentManager(), "profile");

// 변경
ProfileDialog.newInstance().show(getSupportFragmentManager(), "profile");
```

`isLeader` 변수도 더 이상 필요 없으므로 `btnMy.setOnClickListener` 블록을:
```java
btnMy.setOnClickListener(v ->
        ProfileDialog.newInstance().show(getSupportFragmentManager(), "profile"));
```

- [ ] **Step 3: ProfileDialog에 회원탈퇴 버튼 로직 추가**

`onCreateView`의 로그아웃 버튼 설정 코드 (`btnLogout.setOnClickListener(...)`) 아래에 추가:

```java
Button btnDeleteAccount = view.findViewById(R.id.btn_delete_account);
btnDeleteAccount.setOnClickListener(v -> {
    android.view.View confirmView = android.view.LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_task, null);
    ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_title))
            .setText("회원탈퇴");
    ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_message))
            .setText("정말 탈퇴하시겠습니까?\n팀 멤버십과 작업 정보가 초기화됩니다.");
    ((android.widget.TextView) confirmView.findViewById(R.id.btn_dialog_confirm))
            .setText("탈퇴");

    android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(requireContext())
            .setView(confirmView)
            .create();
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
                            ToastUtils.show(requireContext(), "회원탈퇴에 실패했습니다.");
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

- [ ] **Step 4: 빌드 확인**

```bash
cd B:/NudgeUp/app
./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/ui/main/dialog/ProfileDialog.java \
        app/app/src/main/java/com/nudgeup/app/ui/main/MainActivity.java
git commit -m "feat: ProfileDialog 역할 배지 제거, 회원탈퇴 기능 추가"
```

---

## Task 10: 전체 빌드 확인 및 PR 생성

- [ ] **Step 1: 백엔드 전체 테스트**

```bash
cd B:/NudgeUp/backend
./gradlew test --tests "com.nudgeup.backend.domain.task.entity.TaskTest" 2>&1 | tail -3
./gradlew compileJava 2>&1 | tail -3
```

- [ ] **Step 2: 앱 전체 빌드**

```bash
cd B:/NudgeUp/app
./gradlew assembleDebug 2>&1 | tail -3
```

- [ ] **Step 3: push 및 PR 생성**

```bash
git push -u origin feat/team-join-profile
gh pr create --base main \
  --title "feat: 팀 참가 UI 개선 + 프로필 역할 제거 + 회원탈퇴" \
  --body "## Summary
- + 버튼: 팀 생성 or 초대코드 입장 선택 다이얼로그 (다크 테마)
- 초대코드 UI: 다크 테마 커스텀 다이얼로그로 통일 (팀 있을 때도 사용 가능)
- ProfileDialog: 팀장/팀원 역할 배지 제거
- ProfileDialog: 회원탈퇴 버튼 추가 (확인 다이얼로그 → API 호출 → 토큰 삭제 → 로그인 화면)
- 백엔드: DELETE /api/v1/users/me (TeamMember, Task 연결 해제, NudgeLog 삭제, User 삭제)

## Test plan
- [ ] 팀 있을 때 + 버튼 → 다이얼로그 표시 (팀 만들기 / 초대코드 입장)
- [ ] 팀 없을 때 빈 화면 '초대코드로 입장' → 같은 다크 테마 다이얼로그
- [ ] 초대코드 입력 → 팀 합류 확인
- [ ] MY → 팀장/팀원 배지 없는지 확인
- [ ] MY → 회원탈퇴 클릭 → 확인 다이얼로그 → 탈퇴 → 로그인 화면
- [ ] 탈퇴 후 DB에서 유저 삭제 확인 (pgAdmin)"
```
