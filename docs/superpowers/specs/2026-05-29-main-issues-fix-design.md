# NudgeUp 메인 이슈 수정 설계

**날짜**: 2026-05-29  
**브랜치**: fix/main-issues

---

## 1. 팀 생성 실패 (Fix 1)

### 원인
Android `CreateTeamRequest`의 필드명이 `name`이지만 백엔드는 `teamName`을 요구함. `@NotBlank` 검증 실패로 400 에러 발생.

### 수정
`app/app/src/main/java/com/nudgeup/app/data/dto/request/CreateTeamRequest.java`
- `private String name` → `private String teamName`
- 생성자 파라미터명도 동일하게 변경

---

## 2. 빈 팀 Empty State UI (Fix 2)

### 원인
`MainViewModel.loadTeams()` 결과가 빈 리스트일 때 표시할 UI 없음.

### 아키텍처 결정
단일 `activity_main.xml` 안에서 visibility 토글. 별도 Activity 분리 없음.

### 변경 파일

#### `activity_main.xml`
- 오른쪽 메인 콘텐츠 영역(`LinearLayout layout_weight=1`)의 직계 자식들을 `FrameLayout`으로 감쌈
- 기존 콘텐츠 그룹: `id=layout_main_content` (상단바 + 필터 + 스크롤 + 하단바)
- 빈 상태 뷰: `id=layout_empty_state` (기본 GONE)
  - 텍스트: "아직 속한 팀이 없어요"
  - 버튼 1: "새 팀 만들기" → `CreateTeamActivity` 실행
  - 버튼 2: "초대 코드로 입장" → 초대 코드 입력 AlertDialog

#### `MainActivity.java`
- `viewModel.getTeams().observe()` 내에서:
  ```
  boolean isEmpty = list == null || list.isEmpty();
  layoutMainContent.setVisibility(isEmpty ? GONE : VISIBLE);
  layoutEmptyState.setVisibility(isEmpty ? VISIBLE : GONE);
  ```
- "초대 코드로 입장" 클릭 시 `AlertDialog` + `EditText`로 코드 입력 → `viewModel.joinTeam(code)` 호출
- 팀 참가 성공 LiveData 관찰 → 성공 시 `loadTeams()` 재호출

#### `MainViewModel.java`
- `joinTeam(String code)` 메서드 추가
- `MutableLiveData<Boolean> joinResult` 추가

#### `TeamApi.java`
- `@POST("teams/join") Call<ApiResponse<TeamResponse>> joinTeam(@Body JoinTeamRequest request)` 추가

#### `TeamRepository.java`
- `joinTeam(String inviteCode)` 메서드 추가

#### `JoinTeamRequest.java` (신규)
- `private String inviteCode` 필드

---

## 3. ProfileDialog 이름 로딩 + 주 1회 변경 제한 + 서버 연동 (Fix 3)

### 원인
`ProfileDialog`가 이름을 SharedPreferences → TokenManager 순서로만 읽음. 첫 로그인 직후 async write 미완료 등으로 이름이 빈값으로 표시됨.

### 수정

#### `UserApi.java`
```java
@PATCH("users/me")
Call<ApiResponse<UserResponse>> updateProfile(@Body UpdateUserRequest request);
```

#### `UpdateUserRequest.java` (신규 Android DTO)
```java
public class UpdateUserRequest {
    private String username;
    public UpdateUserRequest(String username) { this.username = username; }
}
```

#### `UserRepository.java`
- `updateProfile(String username)` 메서드 추가

#### `ProfileDialog.java`
1. **이름 로딩**: `onCreateView()`에서 `UserRepository.getMe()` 비동기 호출 → 성공 시 `currentName` 업데이트 + `refreshNameDisplay()`
2. **주 1회 제한**:
   - `profile_prefs`에 `KEY_LAST_NAME_CHANGE_MS` (long) 저장
   - 저장 버튼 클릭 시 `System.currentTimeMillis() - lastChange < 7일(ms)` 검사
   - 조건 위반 시 토스트 "이름은 일주일에 한 번만 변경 가능합니다" 표시 후 중단
3. **서버 저장**: `UserRepository.updateProfile(newName)` 호출 → 성공 시 `TokenManager.saveUsername()` 업데이트, `profile_prefs.profile_name` 업데이트, `last_name_change_ms` 저장

---

## 병렬 처리 계획

세 수정은 변경 파일이 완전히 독립적이므로 동시 작업 가능:
- Agent A: Fix 1 (1개 파일)
- Agent B: Fix 2 (5개 파일 + 1개 신규)
- Agent C: Fix 3 (4개 파일 + 1개 신규)
