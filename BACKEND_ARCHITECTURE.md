# NudgeUp 백엔드 아키텍처 설계

## 기술 스택
- **Framework**: Spring Boot 3
- **Language**: Java 17
- **DB**: PostgreSQL 16
- **인증**: JWT (Spring Security)
- **푸시 알림**: FCM (Firebase Cloud Messaging)
- **빌드**: Gradle
- **컨테이너**: Docker / Docker Compose

---

## 패키지 구조

도메인 주도 패키지 구조 (Domain-Driven Package Structure)

```
nudgeup-backend/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/nudgeup/
│   │   │       │
│   │   │       ├── NudgeUpApplication.java
│   │   │       │
│   │   │       ├── domain/                          # 비즈니스 도메인 (핵심 기능)
│   │   │       │   │
│   │   │       │   ├── auth/                        # 인증 도메인
│   │   │       │   │   ├── controller/
│   │   │       │   │   │   └── AuthController.java
│   │   │       │   │   ├── service/
│   │   │       │   │   │   └── AuthService.java
│   │   │       │   │   └── dto/
│   │   │       │   │       ├── request/
│   │   │       │   │       │   ├── LoginRequest.java
│   │   │       │   │       │   ├── RegisterRequest.java
│   │   │       │   │       │   └── TokenRefreshRequest.java
│   │   │       │   │       └── response/
│   │   │       │   │           ├── LoginResponse.java
│   │   │       │   │           └── TokenResponse.java
│   │   │       │   │
│   │   │       │   ├── user/                        # 사용자 도메인
│   │   │       │   │   ├── controller/
│   │   │       │   │   │   └── UserController.java
│   │   │       │   │   ├── service/
│   │   │       │   │   │   └── UserService.java
│   │   │       │   │   ├── repository/
│   │   │       │   │   │   └── UserRepository.java
│   │   │       │   │   ├── entity/
│   │   │       │   │   │   └── User.java
│   │   │       │   │   └── dto/
│   │   │       │   │       ├── request/
│   │   │       │   │       │   ├── UpdateUserRequest.java
│   │   │       │   │       │   └── FcmTokenRequest.java
│   │   │       │   │       └── response/
│   │   │       │   │           └── UserResponse.java
│   │   │       │   │
│   │   │       │   ├── team/                        # 팀 도메인
│   │   │       │   │   ├── controller/
│   │   │       │   │   │   └── TeamController.java
│   │   │       │   │   ├── service/
│   │   │       │   │   │   └── TeamService.java
│   │   │       │   │   ├── repository/
│   │   │       │   │   │   ├── TeamRepository.java
│   │   │       │   │   │   └── TeamMemberRepository.java
│   │   │       │   │   ├── entity/
│   │   │       │   │   │   ├── Team.java
│   │   │       │   │   │   ├── TeamMember.java
│   │   │       │   │   │   └── TeamRole.java        # Enum: LEADER, MEMBER
│   │   │       │   │   └── dto/
│   │   │       │   │       ├── request/
│   │   │       │   │       │   ├── CreateTeamRequest.java
│   │   │       │   │       │   ├── JoinTeamRequest.java
│   │   │       │   │       │   └── UpdateTeamSettingsRequest.java
│   │   │       │   │       └── response/
│   │   │       │   │           ├── TeamResponse.java
│   │   │       │   │           ├── TeamDetailResponse.java
│   │   │       │   │           ├── TeamMemberResponse.java
│   │   │       │   │           └── InviteResponse.java
│   │   │       │   │
│   │   │       │   ├── task/                        # 작업 도메인
│   │   │       │   │   ├── controller/
│   │   │       │   │   │   └── TaskController.java
│   │   │       │   │   ├── service/
│   │   │       │   │   │   └── TaskService.java
│   │   │       │   │   ├── repository/
│   │   │       │   │   │   └── TaskRepository.java
│   │   │       │   │   ├── entity/
│   │   │       │   │   │   ├── Task.java
│   │   │       │   │   │   └── TaskStatus.java      # Enum: PENDING, IN_PROGRESS, COMPLETED
│   │   │       │   │   └── dto/
│   │   │       │   │       ├── request/
│   │   │       │   │       │   ├── CreateTaskRequest.java
│   │   │       │   │       │   └── UpdateTaskStatusRequest.java
│   │   │       │   │       └── response/
│   │   │       │   │           └── TaskResponse.java
│   │   │       │   │
│   │   │       │   └── nudge/                       # 독촉 알림 도메인
│   │   │       │       ├── controller/
│   │   │       │       │   └── NudgeController.java
│   │   │       │       ├── service/
│   │   │       │       │   └── NudgeService.java
│   │   │       │       ├── repository/
│   │   │       │       │   └── NudgeLogRepository.java
│   │   │       │       ├── entity/
│   │   │       │       │   └── NudgeLog.java
│   │   │       │       └── dto/
│   │   │       │           └── response/
│   │   │       │               ├── NudgeResponse.java
│   │   │       │               └── NudgeLogResponse.java
│   │   │       │
│   │   │       └── global/                          # 전역 공통 모듈
│   │   │           │
│   │   │           ├── config/
│   │   │           │   ├── SecurityConfig.java      # Spring Security 설정
│   │   │           │   ├── WebConfig.java           # CORS 등 웹 설정
│   │   │           │   └── FcmConfig.java           # FCM 초기화 설정
│   │   │           │
│   │   │           ├── security/
│   │   │           │   ├── jwt/
│   │   │           │   │   ├── JwtProvider.java     # 토큰 생성/검증
│   │   │           │   │   ├── JwtFilter.java       # 요청마다 토큰 검증 필터
│   │   │           │   │   └── JwtProperties.java   # secret, expiry 설정값
│   │   │           │   ├── CustomUserDetails.java
│   │   │           │   └── CustomUserDetailsService.java
│   │   │           │
│   │   │           ├── exception/
│   │   │           │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   │           │   ├── CustomException.java
│   │   │           │   └── ErrorCode.java               # Enum: 에러 코드 + 메시지
│   │   │           │
│   │   │           ├── response/
│   │   │           │   └── ApiResponse.java             # 공통 응답 래퍼
│   │   │           │
│   │   │           └── fcm/
│   │   │               ├── FcmService.java              # FCM 메시지 발송 로직
│   │   │               └── FcmMessageDto.java           # FCM 페이로드 DTO
│   │   │
│   │   └── resources/
│   │       ├── application.yml                      # 공통 설정
│   │       ├── application-dev.yml                  # 개발 환경 설정
│   │       ├── application-prod.yml                 # 운영 환경 설정
│   │       └── firebase-service-account.json        # FCM 서비스 계정 키 (gitignore)
│   │
│   └── test/
│       └── java/com/nudgeup/
│           ├── domain/
│           │   ├── auth/
│           │   │   └── AuthServiceTest.java
│           │   ├── team/
│           │   │   └── TeamServiceTest.java
│           │   ├── task/
│           │   │   └── TaskServiceTest.java
│           │   └── nudge/
│           │       └── NudgeServiceTest.java
│           └── global/
│               └── security/
│                   └── JwtProviderTest.java
│
├── docker-compose.yml
├── Dockerfile
├── build.gradle
├── settings.gradle
├── .gitignore
└── README.md
```

---

## 레이어별 역할

### Controller
- HTTP 요청/응답 처리
- 요청 유효성 검사 (`@Valid`)
- `@PreAuthorize` 로 권한 체크
- Service 호출 후 `ApiResponse`로 감싸서 반환

### Service
- 핵심 비즈니스 로직 담당
- 권한 검증 (팀장 여부, 담당자 여부 등)
- Repository 조합 및 트랜잭션 관리 (`@Transactional`)
- FCM 발송은 `FcmService`에 위임

### Repository
- Spring Data JPA 인터페이스
- 복잡한 쿼리는 `@Query` (JPQL) 또는 QueryDSL 사용

### Entity
- JPA 엔티티 클래스
- DB 테이블과 1:1 매핑
- 상태 변경 메서드를 엔티티 내부에 캡슐화

---

## 주요 Entity 관계

```
User ──────────── TeamMember ──────────── Team
 │                    │                    │
 │              (role, canNudge,           │
 │               joinedAt)                │
 │                                        │
 └────────────── Task ───────────────────┘
      (assignee, createdBy, teamId)
           │
           │
        NudgeLog
      (taskId, sentBy, sentAt)
```

---

## 핵심 파일 설명

### `ApiResponse.java`
모든 API 응답을 감싸는 공통 래퍼

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
```

### `ErrorCode.java`
에러 코드를 Enum으로 중앙 관리

```java
public enum ErrorCode {
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    TEAM_NOT_FOUND(404, "팀을 찾을 수 없습니다."),
    TASK_NOT_FOUND(404, "작업을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),
    INVALID_INVITE_CODE(400, "유효하지 않은 초대 코드입니다."),
    ALREADY_TEAM_MEMBER(409, "이미 소속된 팀입니다."),
    NO_NUDGE_PERMISSION(403, "독촉 권한이 없습니다."),
    NOT_TEAM_LEADER(403, "팀장만 수행할 수 있습니다."),
    TASK_ALREADY_COMPLETED(400, "이미 완료된 작업입니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다.");

    private final int status;
    private final String message;
}
```

### `TaskStatus.java`
```java
public enum TaskStatus {
    PENDING,       // 대기
    IN_PROGRESS,   // 진행 중
    COMPLETED      // 완료
}
```

### `TeamRole.java`
```java
public enum TeamRole {
    LEADER,   // 팀장
    MEMBER    // 팀원
}
```

---

## application.yml 구조

```yaml
# application.yml (공통)
spring:
  application:
    name: nudgeup
  profiles:
    active: dev
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 3600000      # 1시간
  refresh-token-expiry: 1209600000  # 14일

fcm:
  credentials-path: classpath:firebase-service-account.json

---
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nudgeup
    username: nudgeup
    password: nudgeup
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

---
# application-prod.yml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
```

---

## docker-compose.yml

```yaml
version: '3.8'

services:
  db:
    image: postgres:16
    container_name: nudgeup-db
    environment:
      POSTGRES_DB: nudgeup
      POSTGRES_USER: nudgeup
      POSTGRES_PASSWORD: nudgeup
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  pgadmin:
    image: dpage/pgadmin4
    container_name: nudgeup-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@nudgeup.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - db

  app:
    build: .
    container_name: nudgeup-app
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://db:5432/nudgeup
      DB_USERNAME: nudgeup
      DB_PASSWORD: nudgeup
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      - db

volumes:
  postgres-data:
```

---

## build.gradle 의존성

```groovy
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // PostgreSQL
    runtimeOnly 'org.postgresql:postgresql'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'

    // FCM
    implementation 'com.google.firebase:firebase-admin:9.2.0'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

---

## API → Controller 매핑

| API | Controller 클래스 | 메서드 |
|-----|------------------|--------|
| `POST /auth/register` | `AuthController` | `register()` |
| `POST /auth/login` | `AuthController` | `login()` |
| `POST /auth/refresh` | `AuthController` | `refresh()` |
| `POST /auth/logout` | `AuthController` | `logout()` |
| `GET /users/me` | `UserController` | `getMyProfile()` |
| `PATCH /users/me` | `UserController` | `updateMyProfile()` |
| `PUT /users/me/fcm-token` | `UserController` | `updateFcmToken()` |
| `POST /teams` | `TeamController` | `createTeam()` |
| `GET /teams` | `TeamController` | `getMyTeams()` |
| `GET /teams/{teamId}` | `TeamController` | `getTeamDetail()` |
| `GET /teams/{teamId}/invite` | `TeamController` | `getInviteCode()` |
| `POST /teams/join` | `TeamController` | `joinTeam()` |
| `PATCH /teams/{teamId}/settings` | `TeamController` | `updateTeamSettings()` |
| `DELETE /teams/{teamId}/members/me` | `TeamController` | `leaveTeam()` |
| `POST /teams/{teamId}/tasks` | `TaskController` | `createTask()` |
| `GET /teams/{teamId}/tasks` | `TaskController` | `getTeamTasks()` |
| `GET /teams/{teamId}/tasks/my` | `TaskController` | `getMyTasks()` |
| `PATCH /teams/{teamId}/tasks/{taskId}/status` | `TaskController` | `updateTaskStatus()` |
| `POST /teams/{teamId}/tasks/{taskId}/nudge` | `NudgeController` | `sendNudge()` |
| `GET /teams/{teamId}/nudge-logs` | `NudgeController` | `getNudgeLogs()` |
