# NudgeUp 기능 개발 및 버그 수정 설계서

날짜: 2026-05-31

---

## 브랜치 구성 (접근법 A)

| 브랜치 | 유형 | 내용 |
|---|---|---|
| `fix/status-revert-and-badge-text` | 버그 수정 | 완료 상태 복구 + 기한초과 텍스트 |
| `feat/task-cancel-delete` | 신규 기능 | CANCELLED 상태 + 폐기 버튼 + 3일 자동 만료 |
| `feat/nudge-inbox` | 신규 기능 | 독촉 수신함 화면 + 받은 독촉 카운트 |

---

## 브랜치 1: `fix/status-revert-and-badge-text`

### 목표
- COMPLETED 상태에서 IN_PROGRESS로 되돌릴 수 있게 허용
- 기한이 지난 작업의 배지 텍스트를 "마감임박" → "기한초과"로 수정

### 변경 파일

#### 백엔드: `Task.java`
- `changeStatus()` 메서드에서 COMPLETED → IN_PROGRESS 전환 허용
- 기존: COMPLETED이면 무조건 `TASK_ALREADY_COMPLETED` 예외
- 변경: COMPLETED에서 IN_PROGRESS로의 전환만 예외 제외
  - (추후 feat/task-cancel-delete 머지 후 CANCELLED도 허용 추가)

```java
// 변경 전
if (this.status == TaskStatus.COMPLETED) {
    throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
}

// 변경 후 (fix 브랜치 기준 — CANCELLED 상태 없으므로 IN_PROGRESS만)
if (this.status == TaskStatus.COMPLETED && newStatus != TaskStatus.IN_PROGRESS) {
    throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
}
```

#### 앱: `TaskListAdapter.java`
- `dday < 0`이고 미완료 상태일 때 배지 텍스트: `"마감임박"` → `"기한초과"`
- 상태 바 색상, 배지 배경 리소스는 기존 urgent 스타일 유지

---

## 브랜치 2: `feat/task-cancel-delete`

### 목표
- 모든 상태의 작업에 폐기(CANCELLED) 기능 추가
- COMPLETED/CANCELLED 후 3일이 지난 작업 자동 DB 삭제

### CANCELLED 상태 정의
- 상태값: `CANCELLED`
- 표시: 배지 텍스트 "중단", 색상 회색(0xFF666688), 상태 바 어두운 회색
- 전환 규칙:
  - PENDING/IN_PROGRESS/COMPLETED → CANCELLED 가능
  - CANCELLED → 다른 상태 전환 불가 (단, 자동 삭제 전까지는 표시됨)
- Task entity에 `cancelledAt` 타임스탬프 컬럼 추가

### 폐기 버튼 UI (앱)
- `item_task_card.xml`에 삭제(X) 아이콘 버튼 추가 (btnDelete)
- 위치: btnAction 왼쪽
- 표시 조건:
  - CANCELLED 상태: 버튼 숨김 (이미 폐기됨)
  - 그 외 모든 상태: 버튼 표시
- 클릭 시: 확인 AlertDialog → 확인 시 CANCELLED로 상태 변경 API 호출

### 자동 만료 (백엔드)
- Spring `@Scheduled(cron = "0 0 3 * * *")` — 매일 새벽 3시 실행
- 조건: `status = COMPLETED` AND `completedAt < NOW() - 3일`
         OR `status = CANCELLED` AND `cancelledAt < NOW() - 3일`
- 실행: 해당 작업의 NudgeLog 먼저 삭제 → Task 삭제
- 위치: `global/schedule/TaskCleanupScheduler.java`

### 백엔드 변경 파일
- `TaskStatus.java`: `CANCELLED` 추가
- `Task.java`: `cancelledAt` 필드 추가, `changeStatus()`에서 CANCELLED 처리, COMPLETED → CANCELLED 허용
- `TaskCleanupScheduler.java`: 신규 생성
- `TaskRepository.java`: 만료 대상 조회 쿼리 메서드 추가
- `ErrorCode.java`: 필요 시 에러 코드 추가

### 앱 변경 파일
- `TaskListAdapter.java`: CANCELLED 배지/상태 바 렌더링 + 폐기 버튼 로직
- `item_task_card.xml`: btnDelete ImageButton 추가
- `MainViewModel.java`: `cancelTask(long taskId)` 메서드 추가
- `ic_cancel.xml` 또는 기존 아이콘 재사용

---

## 브랜치 3: `feat/nudge-inbox`

### 목표
- 메인 하단 벨 아이콘 → 클릭 가능한 버튼으로 교체
- 클릭 시 `NudgeInboxActivity`로 이동
- 독촉 N회 카운트를 "내가 받은 최근 3일 독촉 개수"로 변경

### 독촉 수신함 UI (`NudgeInboxActivity`)
- 범위: 내가 속한 전체 팀의 독촉 수신 로그
- 각 항목 표시 템플릿:
  ```
  [팀명]  [작업명]
  [보낸사람]님이 독촉을 보냈습니다   D분 전 / N시간 전 / N일 전
  ```
- 최신순 정렬
- 빈 상태: "아직 받은 독촉이 없어요" 텍스트

### 독촉 N회 카운트 변경
- 기존: 클라이언트 메모리에서 내가 보낸 독촉 수 카운트
- 변경: 서버에서 내가 받은 최근 3일 독촉 수 조회
- 팀 선택 변경 또는 화면 재진입 시 갱신

### 백엔드 신규 API

#### 1. 받은 독촉 수 조회
`GET /api/v1/nudges/received/count?days=3`
- 현재 사용자가 assignee인 작업들에 달린 NudgeLog 중 최근 3일치 카운트
- `NudgeLogRepository`에 쿼리 추가
- `UserNudgeController.java` 또는 기존 `NudgeController` 확장

#### 2. 받은 독촉 로그 목록 조회
`GET /api/v1/nudges/received`
- 현재 사용자가 assignee인 작업들의 NudgeLog 전체 목록
- 응답: NudgeLogResponse 리스트 (taskTitle, teamId, teamName, sentBy, sentAt 포함)
- 팀명 포함을 위해 Team 정보 조인 필요

### 앱 변경 파일
- `activity_main.xml`: 벨 ImageView → ImageButton (`id: btn_nudge_inbox`)
- `MainActivity.java`: 벨 버튼 클릭 시 `NudgeInboxActivity` 시작
- `MainViewModel.java`: `getNudgeCount()` → 서버에서 받은 독촉 수 조회로 변경
- `NudgeInboxActivity.java`: 신규 생성
- `activity_nudge_inbox.xml`: 신규 생성
- `item_nudge_log.xml`: 신규 생성
- `NudgeLogAdapter.java`: 신규 생성
- `NudgeRepository.java`: 받은 독촉 API 메서드 추가

---

## 구현 순서

1. `fix/status-revert-and-badge-text` — 가장 작음, 먼저 머지
2. `feat/task-cancel-delete` — fix 머지 후 진행 (CANCELLED 포함하여 Task.changeStatus 완성)
3. `feat/nudge-inbox` — feat/task-cancel-delete와 병렬 진행 가능

---

## FCM 토큰 로직 설명 (코드 변경 없음)

현재 FCM 흐름:
1. 앱 최초 실행 시 `FirebaseMessaging.getInstance().getToken()`으로 기기 토큰 발급
2. 로그인 성공 후 해당 토큰을 `POST /api/v1/auth/fcm-token` 등으로 서버에 저장 (User.fcmToken 컬럼)
3. 독촉 발송 시 `NudgeService`에서 `receiver.getFcmToken()`으로 토큰 조회
4. `FcmService.sendNudgeMessage()`가 Firebase Admin SDK를 통해 해당 기기에 푸시 전송

토큰이 제대로 저장·전송되는지 확인하려면:
- pgAdmin에서 `users` 테이블의 `fcm_token` 컬럼 값 확인
- Firebase Console > Cloud Messaging > 테스트 메시지로 토큰 직접 전송 테스트
- 백엔드 로그에서 `FcmService` 성공/실패 로그 확인
