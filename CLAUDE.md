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
# 프로젝트 계획서
## 1. 앱명

**NudgeUp** (너지업)

### 1-1. 로고

![image.png](attachment:0fdc8f2b-0309-44ce-8a07-fbfe8d81b79b:image.png)

## 2. 앱 설명

- 팀 단위 업무 환경에서 관리자가 팀원에게 작업을 부여하고, 진행이 느린 팀원에게 즉시 푸시 알림으로 독촉 알림을 보낼 수 있는 안드로이드 기반 팀 업무 독촉 관리 앱
- 초대 링크를 통해 팀에 참가할 수 있으며, 팀원은 앱 내에서 본인의 작업 목록을 확인하고 완료 처리할 수 있음

---

## 3. 앱의 특징 및 주요 기능

### 특징

- 초대 링크를 통한 팀 참가 - 별도 승인 절차 없이 즉시 합류
- FCM(Firebase Cloud Messaging) 기반 실시간 푸시 알림으로 독촉 즉시 전달
- 관리자가 직접 팀원에게 작업 부여, 팀원도 독단적으로 작업 생성 가능

### 주요 기능

- 회원가입 / 로그인: 이메일 기반 인증, JWT 토큰으로 세션 유지
- 작업 부여: 관리자가 팀원을 선택해 작업명, 마감일 포함 업무 할당
    - 팀원이 직접 자신의 작업 생성 가능
- 독촉 알림 발송: 관리자가 독촉 버튼 클릭 시 담당 팀원에게 FCM 푸시 알림 즉시 발송
- 작업 상태 변경: 작업 진행자 혹은 팀장이 작업의 상태 변경 가능
    - 작업 상태: 대기, 진행 중, 완료
- 팀 작업 목록 조회: 팀 내 전체 작업 현황 및 진행 상태 확인

---

## 4. UI/UX 설계

### 로그인 화면

#### 화면 설명

앱 시작 시 초기 로그인을 진행하는 화면

- JWT (JSON Web Token)을 이용한 사용자 인증 진행
- 회원가입 연결
- TextView, Button 사용

![image.png](attachment:179ca2f4-7a43-4746-8911-93e0aef6bf38:image.png)

|  | Type | 기능 | 비고 |
| --- | --- | --- | --- |
| 1 | 로그인 버튼 | 로그인 실행 | 메인 화면 이동 |
| 2 | 회원가입 버튼 | 회원가입 시퀀스 실행 | 회원가입 화면 이동 |

### 회원가입 화면

#### 화면 설명

회원 가입을 진행하는 화면

- TextView, EditText, Button, ImageView 사용

![image.png](attachment:0c790ab3-f332-4364-936c-558f7a5c364e:image.png)

|  | Type | 기능 | 비고 |
| --- | --- | --- | --- |
| 1 | 회원가입 버튼 | 회원가입 진행 | 로그인 화면 이동 |
| 2 | 로그인 텍스트 버튼 | 로그인 화면 이동 | 로그인 화면 이동 |

### 메인 화면

#### 화면 설명

NudgeUp의 메인 화면

- 팀 별 보기 가능
- 해당 팀의 팀원, 할 일, 나의 할 일 보기
- RecyclerView, HorizontalScrollView 등의 View 사용

![image.png](attachment:a5111f0b-6326-445f-8b2e-37960b0fd49a:image.png)

|  | Type | 기능 | 비고 |
| --- | --- | --- | --- |
| 1 | 팀 선택 버튼  | 해당 팀의 화면을 보여줌 |  |
| 2 | 독촉 버튼 | 해당 작업자에게 독촉 알림을 보냄 | FCM 사용 |
| 3 | 수정하기 버튼 | 내 할 일의 상태를 변경 |  |
| 4 | 새 작업 버튼 | 새로운 작업 추가  | 작업 생성 화면으로 이동 |
| 5 | My 버튼 | 간단한 내 프로필 조회 | 개인 설정 dialog show |
| 6 | 팀 설정 버튼  | 팀의 간단한 설정 가능 | 팀 설정 dialog show      팀장에게만 보임 |
| 7 | 팀 추가 버튼  | 새 팀 생성 | 팀 생성 dialog show |

### 작업 생성 화면

작업을 생성할 수 있는 화면

#### 화면 설명

- 작업 생성은 누구나 가능
- 작업 담당자를 누구나 지정 가능
- Button, EditText, Dialog 사용

![image.png](attachment:e13ff694-0222-4cb8-9261-292e17ab06e1:image.png)

|  | Type | 기능 | 비고 |
| --- | --- | --- | --- |
| 1 | 담당자 지정 버튼 | 담당자를 지정할 수 있음 | 담당자 지정 dialog show |
| 2 | 마감일 지정 버튼 | 마감일을 지정할 수 있음 | 마감일 지정 dialog show |
| 3 | 취소 버튼 | 현재 생성중인 작업 취소 | 메인 화면 이동 |
| 4 | 생성 버튼 | 입력된 정보를 바탕으로 작업 생성 | 메인 화면 이동 |

### 팀 설정 화면

#### 화면 설명

선택한 팀의 간단한 설정을 조작할 수 있는 화면

- 팀장만 접근 가능
- Spinner, RecyclerView 사용

![image.png](attachment:05e3b837-0a63-469f-a862-c55a74e6795d:image.png)

|  | Type | 기능 | 비고 |
| --- | --- | --- | --- |
| 1 | 닫기 버튼 | 현재 창 닫기 | 메인 화면 이동 |
| 2 | 팀장 지정 드롭박스 | 새로운 팀장 지정 |  |
| 3 | 독촉 권한 on/off toggle | 각 팀원의 독촉 권한 설정 |  |
| 4 | 저장 버튼 | 설정된 정보 저장 | 메인 화면 이동 |

---

## 5. DB 설계

![nudgeup_erd.jpg](attachment:f41b1c37-ec7c-46ae-89f6-7a2ab058a884:nudgeup_erd.jpg)

---

## 6. 개발 환경 및 기술 스택

### SDK / Languages

- Android SDK 36
- Java 17

### Server

- Spring Boot 3
- REST API 방식 통신
- JWT 기반 인증

### DBMS

- PostgreSQL 16

### Design Tool

- Figma

### OpenAPI / 외부 서비스

- FCM (Firebase Cloud Messaging) - 독촉 푸시 알림 발송

### 개발 도구 및 환경

- Android Studio (안드로이드 개발)
- IntelliJ IDEA (백엔드 개발)
- Docker / Docker Compose - 개발 환경 통일 (Spring Boot + PostgreSQL + pgAdmin 컨테이너 구성)
- Git / GitHub - 버전 관리 및 협업

---

## 7. 역할 분담 및 일정

### 역할 분담

- 오동준: 알림 보내기, 팀 생성, 팀 설정, DB 설계
- 이원호: 회원 가입/로그인, 작업 생성, 개인 설정, UX 설계 (Figma)

### 개발 일정

- 1주차: DB 설계 확정, Spring Boot 프로젝트 세팅, Docker 환경 구성
- 2주차: JWT 인증 API 완성, 안드로이드 로그인 화면 구현 및 메인 화면 구현
- 3주차: 알림 기능 완성 및 기능 최적화, 남은 기능 개발
- 4주차: 테스트 실행 및 디버깅, 보고서 작성