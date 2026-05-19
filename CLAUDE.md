# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

NudgeUp(너지업) — 팀 업무 독촉 관리 앱. 관리자가 팀원에게 작업을 부여하고 FCM 푸시로 즉시 독촉할 수 있다.

모노레포 구조:
- `app/` — Android 클라이언트 (Java 17, Android SDK 36)
- `backend/` — Spring Boot 3 REST API 서버 (Java 17, PostgreSQL 16)

## 개발 환경 시작

```bash
# 루트에서 .env 파일 준비 (.env.example 복사 후 값 채우기)
cp .env.example .env

# DB + pgAdmin + 백엔드 컨테이너 전체 실행
docker-compose up -d

# 백엔드만 로컬 실행 (DB는 Docker, 앱은 IDE)
cd backend
./gradlew bootRun
```

**필수 수동 배치 파일** (커밋 금지):
- `backend/src/main/resources/firebase-service-account.json` — Firebase 콘솔에서 발급

## 백엔드 명령어

```bash
cd backend

# 빌드
./gradlew build

# 테스트 전체 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.nudgeup.backend.domain.auth.AuthServiceTest"

# 테스트 제외 JAR 빌드 (Docker 이미지 빌드와 동일)
./gradlew bootJar -x test

# 컴파일만 확인
./gradlew compileJava
```

서버 포트: `8080` / pgAdmin: `http://localhost:5050`

## 백엔드 아키텍처

**패키지 루트**: `com.nudgeup.backend`

### domain 레이어 (비즈니스 도메인)

각 도메인(`auth`, `user`, `team`, `task`, `nudge`)은 동일한 내부 구조를 가진다:

```
domain/{name}/
  controller/   # @RestController — HTTP 매핑, @AuthenticationPrincipal로 userId 추출
  service/      # 핵심 비즈니스 로직, @Transactional 관리
  repository/   # Spring Data JPA 인터페이스
  entity/       # JPA 엔티티 — 상태 변경은 반드시 엔티티 내부 메서드로만
  dto/
    request/    # @Valid 검증 어노테이션 포함
    response/   # 정적 팩토리 메서드 from(Entity) 패턴 사용
```

### global 레이어 (공통 인프라)

| 패키지 | 역할 |
|--------|------|
| `global/response/ApiResponse` | 모든 API 응답 래퍼 (`success()`, `fail()`) |
| `global/exception/ErrorCode` | 전체 에러 코드 Enum (HTTP 상태 + 메시지 포함) |
| `global/exception/GlobalExceptionHandler` | `@RestControllerAdvice` 전역 예외 처리 |
| `global/security/jwt/JwtProvider` | 토큰 생성·검증·파싱 |
| `global/security/jwt/JwtFilter` | 요청마다 토큰 검증, SecurityContext 설정 |
| `global/fcm/FcmService` | FCM 발송 — 실패해도 예외 전파 안 함 (로그만) |

### 핵심 패턴

**에러 처리**: Service에서 `throw new CustomException(ErrorCode.XXX)` → `GlobalExceptionHandler`가 catch → `ApiResponse.fail()` 응답

**권한 검증 흐름**:
1. `JwtFilter` → `SecurityContextHolder`에 `CustomUserDetails` 저장
2. Controller에서 `@AuthenticationPrincipal CustomUserDetails userDetails`로 추출
3. Service에서 `teamMemberRepository.findByTeamIdAndUserId()`로 팀 소속 및 역할 검증

**엔티티 상태 변경**: setter 금지, 엔티티 내부 메서드만 사용 (`task.changeStatus()`, `user.updateProfile()` 등)

**독촉 알림 흐름**: `NudgeController` → `NudgeService` (권한 검증 + `FcmService` 호출 + `NudgeLog` 저장)

### DB 테이블 관계

```
User ─── TeamMember ─── Team
               │
           (role, canNudge)
               │
Task (assignee_id, created_by_id, team_id)
               │
           NudgeLog (task_id, sent_by_id)
```

`TeamMember.role`: `LEADER` / `MEMBER`  
`Task.status`: `PENDING` → `IN_PROGRESS` → `COMPLETED`

## API 기본 경로

`/api/v1/` 접두사 공통. 인증 불필요 경로: `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`

전체 명세: `API_SPEC.md` / 개발 작업 목록: `backend/DEV_TASKS.md`

## 주의사항

- `firebase-service-account.json`은 절대 커밋하지 않는다 (`backend/.gitignore`에 등록됨)
- FCM 관련 import는 `firebase-admin` 의존성 추가 후에만 사용 (`build.gradle.kts`에 추가됨)
- Spring Boot 3 사용으로 `javax.*` 대신 `jakarta.*` 네임스페이스를 사용한다
- `@Transactional(readOnly = true)`를 Service 클래스 기본값으로, 쓰기 메서드에만 `@Transactional` 오버라이드
