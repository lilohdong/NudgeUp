# NudgeUp 백엔드 개발 작업 명세서

> 패키지별로 구현해야 할 작업 목록입니다.
> 각 작업은 독립적으로 커밋할 수 있는 단위로 구성되어 있습니다.

---

## 사전 작업

### [ ] build.gradle.kts — FCM 의존성 추가
```kotlin
implementation("com.google.firebase:firebase-admin:9.2.0")
```

### [ ] application.yaml — JWT / FCM 설정 추가
```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 3600000
  refresh-token-expiry: 1209600000

fcm:
  credentials-path: classpath:firebase-service-account.json
```

### [ ] .gitignore — 민감 파일 제외
```
src/main/resources/firebase-service-account.json
```

### [ ] firebase-service-account.json
- Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → 새 비공개 키 생성
- `src/main/resources/` 에 배치 (절대 커밋 금지)

---

## 1단계: global — 공통 인프라

**커밋 메시지 예시**: `feat: global 공통 모듈 구현 (ApiResponse, ErrorCode, JWT, Security)`

### [ ] `global/response/ApiResponse.java`
- `success(T data)` → `ApiResponse(true, "요청이 성공적으로 처리되었습니다.", data)` 반환
- `success(String message, T data)` → 커스텀 메시지 포함
- `fail(String message)` → `ApiResponse(false, message, null)` 반환

### [ ] `global/exception/ErrorCode.java`
- 이미 모든 에러 코드 정의 완료 ✅ (추가 에러 발생 시 여기에만 추가)

### [ ] `global/exception/CustomException.java`
- 구현 완료 ✅ (Service에서 `throw new CustomException(ErrorCode.XXX)` 형식으로 사용)

### [ ] `global/exception/GlobalExceptionHandler.java`
- `handleCustomException`: `ResponseEntity.status(e.getErrorCode().getStatus()).body(ApiResponse.fail(e.getMessage()))` 반환
- `handleValidationException`: `BindingResult`에서 첫 번째 필드 에러 메시지 추출 후 400 반환
- `handleException`: 500 + "서버 내부 오류" 반환 + `log.error()` 기록

### [ ] `global/security/jwt/JwtProperties.java`
- `@ConfigurationProperties` 바인딩만 하면 됨 (application.yaml 설정과 자동 연결)

### [ ] `global/security/jwt/JwtProvider.java`
- `generateAccessToken(userId)`: `Jwts.builder().claim("userId", userId).expiration(...).signWith(secretKey).compact()`
- `generateRefreshToken(userId)`: 동일 구조, expiry만 다름
- `validateToken(token)`: try-catch로 `Jwts.parser()...parseSignedClaims()` 후 `true` / 예외 시 `false`
- `getUserId(token)`: Claims에서 `"userId"` 추출 후 `Long` 변환
- `getAuthentication(token)`: `CustomUserDetailsService.loadUserByUsername(userId)` 호출 후 `UsernamePasswordAuthenticationToken` 생성

### [ ] `global/security/jwt/JwtFilter.java`
- `doFilterInternal`: `Authorization` 헤더에서 `"Bearer "` 제거 후 토큰 추출
- `validateToken` 통과 시 `SecurityContextHolder.getContext().setAuthentication()` 설정

### [ ] `global/security/CustomUserDetails.java`
- `getPassword()`: `user.getPassword()` 반환
- `getUsername()`: `user.getId().toString()` 반환

### [ ] `global/security/CustomUserDetailsService.java`
- `loadUserByUsername(userId)`: `userRepository.findById(Long.parseLong(userId))` → 없으면 `throw new CustomException(ErrorCode.USER_NOT_FOUND)`

### [ ] `global/config/SecurityConfig.java`
```java
http
  .csrf(AbstractHttpConfigurer::disable)
  .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .authorizeHttpRequests(auth -> auth
      .requestMatchers(POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
      .anyRequest().authenticated()
  )
  .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
```

### [ ] `global/config/WebConfig.java`
- `addCorsMappings`: `registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("*")`

### [ ] `global/config/FcmConfig.java`
- `@PostConstruct initialize()`: `InputStream` 로드 → `FirebaseOptions.builder()` → `FirebaseApp.initializeApp()`

### [ ] `global/fcm/FcmMessageDto.java`
- `Map<String, String> data` 필드 추가 (type, teamId, taskId, taskTitle, sentBy)

### [ ] `global/fcm/FcmService.java`
- `sendNudgeMessage(dto)`: FCM 토큰 null/blank 체크 → `FirebaseMessaging.getInstance().send(message)` 호출
- `FirebaseMessagingException` catch → `log.warn("FCM 발송 실패: {}", e.getMessage())` 후 무시

---

## 2단계: domain/user — 사용자

**커밋 메시지 예시**: `feat: user 도메인 구현 (프로필 조회/수정, FCM 토큰 등록)`

### [ ] `domain/user/entity/User.java`
- `@CreationTimestamp LocalDateTime createdAt` 추가
- `@Builder` 생성자: `color` 기본값 없으면 `"#A78BFA"` 지정
- `updateProfile(username, color)`: null 아닌 값만 업데이트 (null-safe)
- `updateFcmToken(fcmToken)`: `this.fcmToken = fcmToken`

### [ ] `domain/user/dto/response/UserResponse.java`
- `from(User)`: 빌더로 필드 매핑 (password, fcmToken 제외)

### [ ] `domain/user/service/UserService.java`
- `getMyProfile`: `findById` 후 `CustomException(USER_NOT_FOUND)` 또는 `UserResponse.from()` 반환
- `updateMyProfile`: 조회 → `updateProfile()` → `UserResponse.from()`
- `updateFcmToken`: 조회 → `updateFcmToken()` (void)

### [ ] `domain/user/controller/UserController.java`
- 각 메서드: `userDetails.getUser().getId()`로 userId 추출 → service 호출 → `ResponseEntity.ok(ApiResponse.success(...))`

---

## 3단계: domain/auth — 인증

**커밋 메시지 예시**: `feat: auth 도메인 구현 (회원가입, 로그인, JWT 토큰 발급)`

### [ ] `domain/auth/service/AuthService.java`
- `register`:
  1. `existsByEmail` 중복 검사 → `DUPLICATE_EMAIL`
  2. `passwordEncoder.encode()`
  3. `User.builder().color("#A78BFA").build()` → save
  4. `UserResponse.from()` 반환
- `login`:
  1. `findByEmail` → 없으면 `INVALID_CREDENTIALS`
  2. `passwordEncoder.matches` → 불일치 `INVALID_CREDENTIALS`
  3. access/refresh 토큰 발급 → `LoginResponse` 반환
- `refresh`:
  1. `validateToken` 실패 → `INVALID_TOKEN`
  2. `getUserId` → 새 토큰 발급 → `TokenResponse` 반환
- `logout`: 현재 구현 불필요 (클라이언트 측 토큰 삭제로 처리)

### [ ] `domain/auth/controller/AuthController.java`
- `register`: 201 Created 반환
- `login`, `refresh`, `logout`: 200 OK 반환

---

## 4단계: domain/team — 팀

**커밋 메시지 예시**: `feat: team 도메인 구현 (팀 생성, 초대 참가, 설정)`

### [ ] `domain/team/entity/Team.java`
- `@CreationTimestamp LocalDateTime createdAt` 추가
- `@Builder` 생성자 구현
- `updateLeader`, `updateTeamName` 구현

### [ ] `domain/team/entity/TeamMember.java`
- `@CreationTimestamp LocalDateTime joinedAt` 추가
- `@Builder` 생성자 구현
- `updateNudgePermission`, `updateRole` 구현

### [ ] `domain/team/dto/response/TeamMemberResponse.java`
- `from(TeamMember)`: `member.getUser().getUsername()`, `role`, `canNudge`, `joinedAt` 매핑

### [ ] `domain/team/dto/response/TeamDetailResponse.java`
- `from(Team, List<TeamMemberResponse>)`: 팀 정보 + 멤버 목록 조합

### [ ] `domain/team/service/TeamService.java`
- `createTeam`:
  - `inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase()`
  - `Team` 저장 → 생성자를 `LEADER + canNudge=true`로 `TeamMember` 저장
- `getMyTeams`:
  - `findAllByUserId` → 각 `teamId`로 `Team` + `memberCount` 조회 → `TeamResponse` 리스트
- `getTeamDetail`:
  - 소속 검증 → `findByTeamId`로 멤버 목록 → `TeamDetailResponse.from()`
- `joinTeam`:
  - 초대 코드 조회 → 중복 검사 → `MEMBER + canNudge=false`로 저장
- `updateTeamSettings`:
  - 팀장 검증 → teamName/newLeader/permissions 조건부 적용
- `leaveTeam`:
  - 팀장이면 `LEADER_CANNOT_LEAVE` → `delete(teamMember)`

### [ ] `domain/team/controller/TeamController.java`
- 각 메서드: userId 추출 → service 호출 → 적절한 HTTP 상태 반환
- `createTeam`: 201 Created
- 나머지: 200 OK

---

## 5단계: domain/task — 작업

**커밋 메시지 예시**: `feat: task 도메인 구현 (작업 생성, 조회, 상태 변경)`

### [ ] `domain/task/entity/Task.java`
- `@CreationTimestamp LocalDateTime createdAt` 추가
- `@Builder` 생성자: `status` 기본값 `= TaskStatus.PENDING`
- `changeStatus(newStatus)`:
  - `IN_PROGRESS` → `this.startedAt = LocalDateTime.now()`
  - `COMPLETED` → `this.completedAt = LocalDateTime.now()`

### [ ] `domain/task/dto/response/TaskResponse.java`
- `from(Task, UserResponse assignee, UserResponse createdBy, long nudgeCount)`:
  - `dDay = task.getDueDate() != null ? ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDate()) : null`

### [ ] `domain/task/service/TaskService.java`
- `createTask`:
  - `assigneeId == null` → `userId` 사용
  - `assigneeId != null` → 해당 유저가 팀 소속인지 검증
  - `Task` 저장 → 조회 → `TaskResponse.from()` 반환
- `getTeamTasks`: status 파라미터에 따라 `findByTeamId` 또는 `findByTeamIdAndStatus` 호출
- `getMyTasks`: assigneeId = userId 고정
- `updateTaskStatus`:
  - 담당자(`task.getAssigneeId() == userId`) 또는 팀장 여부 검증 → `NO_TASK_PERMISSION`
  - `task.changeStatus()` → `TaskResponse.from()`

### [ ] `domain/task/controller/TaskController.java`
- `createTask`: 201 Created
- 나머지: 200 OK

---

## 6단계: domain/nudge — 독촉 알림

**커밋 메시지 예시**: `feat: nudge 도메인 구현 (FCM 독촉 알림 발송, 로그 조회)`

### [ ] `domain/nudge/entity/NudgeLog.java`
- `@CreationTimestamp LocalDateTime sentAt` 추가
- `@Builder` 생성자 구현

### [ ] `domain/nudge/repository/NudgeLogRepository.java`
- `findByTaskIdIn(@Query)` 구현: 팀 전체 작업 로그 조회용

### [ ] `domain/nudge/dto/response/NudgeLogResponse.java`
- `from(NudgeLog, taskTitle, sentTo, sentBy)` 구현

### [ ] `domain/nudge/service/NudgeService.java`
- `sendNudge`:
  1. `taskRepository.findById` → `TASK_NOT_FOUND`
  2. `task.getStatus() == COMPLETED` → `TASK_ALREADY_COMPLETED`
  3. 발신자 멤버십 조회 → `NOT_TEAM_MEMBER`
  4. `LEADER || canNudge` 검증 → `NO_NUDGE_PERMISSION`
  5. `assignee` User 조회 → `fcmToken` 확인
  6. `FcmMessageDto` 구성 → `fcmService.sendNudgeMessage()` 호출
  7. `nudgeLogRepository.save(NudgeLog.builder()...)`
  8. `NudgeResponse` 반환
- `getNudgeLogs`:
  - `taskId != null` → `findByTaskId()`
  - `taskId == null` → 팀 전체 작업 ID 수집 → `findByTaskIdIn()`

### [ ] `domain/nudge/controller/NudgeController.java`
- 각 메서드: userId 추출 → service 호출 → 200 OK 반환

---

## 7단계: 테스트

**커밋 메시지 예시**: `test: 서비스 단위 테스트 작성`

### [ ] `AuthServiceTest.java`
- 회원가입 성공 / 이메일 중복 실패
- 로그인 성공 / 이메일 없음 실패 / 비밀번호 불일치 실패
- 토큰 갱신 성공 / 유효하지 않은 토큰 실패

### [ ] `TeamServiceTest.java`
- 팀 생성 / 초대 코드 참가 / 중복 참가 실패
- 팀장 설정 수정 / 팀원이 설정 수정 시도 실패
- 팀장 탈퇴 시도 실패 / 팀원 탈퇴 성공

### [ ] `TaskServiceTest.java`
- 작업 생성 / 담당자 미지정 시 본인 할당
- 상태 변경 성공 / 권한 없는 유저 상태 변경 실패

### [ ] `NudgeServiceTest.java`
- 독촉 발송 성공 / 완료된 작업 독촉 실패
- 독촉 권한 없는 팀원 발송 실패

### [ ] `JwtProviderTest.java`
- 토큰 생성 / 검증 / userId 추출 / 만료 토큰 검증 실패

---

## 개발 순서 권장

```
1단계 global  →  2단계 user  →  3단계 auth
     ↓
4단계 team  →  5단계 task  →  6단계 nudge
     ↓
7단계 test
```

> 각 단계가 이전 단계에 의존하므로 순서를 지켜서 구현하세요.
> `auth` 완료 후 Postman으로 토큰 발급 테스트를 선행하면 이후 개발이 수월합니다.
