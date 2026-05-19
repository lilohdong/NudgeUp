# NudgeUp API 명세서

## 공통 사항

- **Base URL**: `http://localhost:8080/api/v1`
- **인증 방식**: JWT Bearer Token
- **Content-Type**: `application/json`
- **인증이 필요한 API**: 요청 헤더에 `Authorization: Bearer {accessToken}` 포함

### 공통 응답 형식

```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... }
}
```

### 공통 에러 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

### HTTP 상태 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 (유효성 검사 실패) |
| 401 | 인증 실패 (토큰 없음 또는 만료) |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복 데이터 충돌 |
| 500 | 서버 내부 오류 |

---

## 1. 인증 API (`/auth`)

### 1-1. 회원가입

**POST** `/auth/register`

인증 불필요

**Request Body**

```json
{
  "username": "홍길동",
  "email": "user@example.com",
  "password": "password123!"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| username | String | Y | 사용자 이름 |
| email | String | Y | 이메일 (중복 불가) |
| password | String | Y | 비밀번호 (8자 이상) |

**Response `201`**

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": 1,
    "username": "홍길동",
    "email": "user@example.com",
    "color": "#A78BFA"
  }
}
```

**Error Cases**

| 상태 코드 | 메시지 |
|-----------|--------|
| 400 | 이메일 형식이 올바르지 않습니다. |
| 409 | 이미 사용 중인 이메일입니다. |

---

### 1-2. 로그인

**POST** `/auth/login`

인증 불필요

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "로그인이 완료되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5...",
    "user": {
      "userId": 1,
      "username": "홍길동",
      "email": "user@example.com",
      "color": "#A78BFA"
    }
  }
}
```

**Error Cases**

| 상태 코드 | 메시지 |
|-----------|--------|
| 401 | 이메일 또는 비밀번호가 올바르지 않습니다. |

---

### 1-3. 토큰 갱신

**POST** `/auth/refresh`

인증 불필요

**Request Body**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5..."
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "토큰이 갱신되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5..."
  }
}
```

**Error Cases**

| 상태 코드 | 메시지 |
|-----------|--------|
| 401 | 유효하지 않거나 만료된 토큰입니다. |

---

### 1-4. 로그아웃

**POST** `/auth/logout`

인증 필요

**Request Body** 없음

**Response `200`**

```json
{
  "success": true,
  "message": "로그아웃되었습니다.",
  "data": null
}
```

---

## 2. 사용자 API (`/users`)

### 2-1. 내 프로필 조회

**GET** `/users/me`

인증 필요

**Response `200`**

```json
{
  "success": true,
  "message": "프로필 조회 성공",
  "data": {
    "userId": 1,
    "username": "홍길동",
    "email": "user@example.com",
    "color": "#A78BFA"
  }
}
```

---

### 2-2. 내 프로필 수정

**PATCH** `/users/me`

인증 필요

**Request Body**

```json
{
  "username": "새이름",
  "color": "#34D399"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| username | String | N | 변경할 이름 |
| color | String | N | 프로필 색상 (HEX) |

**Response `200`**

```json
{
  "success": true,
  "message": "프로필이 수정되었습니다.",
  "data": {
    "userId": 1,
    "username": "새이름",
    "email": "user@example.com",
    "color": "#34D399"
  }
}
```

---

### 2-3. FCM 토큰 등록/갱신

**PUT** `/users/me/fcm-token`

인증 필요

푸시 알림 수신을 위해 앱 실행 시 등록

**Request Body**

```json
{
  "fcmToken": "fME3xq2r8T..."
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "FCM 토큰이 등록되었습니다.",
  "data": null
}
```

---

## 3. 팀 API (`/teams`)

### 3-1. 팀 생성

**POST** `/teams`

인증 필요 | 생성한 사용자가 자동으로 팀장이 됨

**Request Body**

```json
{
  "teamName": "프로젝트팀 A"
}
```

**Response `201`**

```json
{
  "success": true,
  "message": "팀이 생성되었습니다.",
  "data": {
    "teamId": 1,
    "teamName": "프로젝트팀 A",
    "inviteCode": "ABC123XY",
    "inviteLink": "nudgeup://join?code=ABC123XY",
    "leaderId": 1,
    "createdAt": "2026-05-19T10:00:00"
  }
}
```

---

### 3-2. 내 팀 목록 조회

**GET** `/teams`

인증 필요

**Response `200`**

```json
{
  "success": true,
  "message": "팀 목록 조회 성공",
  "data": [
    {
      "teamId": 1,
      "teamName": "프로젝트팀 A",
      "leaderId": 1,
      "memberCount": 4,
      "myRole": "LEADER",
      "createdAt": "2026-05-19T10:00:00"
    },
    {
      "teamId": 2,
      "teamName": "스터디 B",
      "leaderId": 3,
      "memberCount": 6,
      "myRole": "MEMBER",
      "createdAt": "2026-05-18T09:00:00"
    }
  ]
}
```

---

### 3-3. 팀 상세 조회

**GET** `/teams/{teamId}`

인증 필요 | 해당 팀 소속 멤버만 가능

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| teamId | Long | 팀 ID |

**Response `200`**

```json
{
  "success": true,
  "message": "팀 조회 성공",
  "data": {
    "teamId": 1,
    "teamName": "프로젝트팀 A",
    "leaderId": 1,
    "inviteCode": "ABC123XY",
    "createdAt": "2026-05-19T10:00:00",
    "members": [
      {
        "userId": 1,
        "username": "홍길동",
        "color": "#A78BFA",
        "role": "LEADER",
        "canNudge": true,
        "joinedAt": "2026-05-19T10:00:00"
      },
      {
        "userId": 2,
        "username": "김철수",
        "color": "#34D399",
        "role": "MEMBER",
        "canNudge": false,
        "joinedAt": "2026-05-19T11:00:00"
      }
    ]
  }
}
```

---

### 3-4. 초대 링크 조회

**GET** `/teams/{teamId}/invite`

인증 필요 | 해당 팀 소속 멤버만 가능

**Response `200`**

```json
{
  "success": true,
  "message": "초대 코드 조회 성공",
  "data": {
    "inviteCode": "ABC123XY",
    "inviteLink": "nudgeup://join?code=ABC123XY"
  }
}
```

---

### 3-5. 초대 코드로 팀 참가

**POST** `/teams/join`

인증 필요 | 별도 승인 없이 즉시 참가

**Request Body**

```json
{
  "inviteCode": "ABC123XY"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "팀에 참가했습니다.",
  "data": {
    "teamId": 1,
    "teamName": "프로젝트팀 A",
    "role": "MEMBER",
    "joinedAt": "2026-05-19T12:00:00"
  }
}
```

**Error Cases**

| 상태 코드 | 메시지 |
|-----------|--------|
| 400 | 유효하지 않은 초대 코드입니다. |
| 409 | 이미 소속된 팀입니다. |

---

### 3-6. 팀 설정 수정

**PATCH** `/teams/{teamId}/settings`

인증 필요 | **팀장만 가능**

**Request Body**

```json
{
  "teamName": "새 팀 이름",
  "newLeaderId": 2,
  "memberPermissions": [
    { "userId": 2, "canNudge": true },
    { "userId": 3, "canNudge": false },
    { "userId": 4, "canNudge": true }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| teamName | String | N | 변경할 팀 이름 |
| newLeaderId | Long | N | 새 팀장으로 지정할 유저 ID |
| memberPermissions | Array | N | 팀원별 독촉 권한 목록 |
| memberPermissions[].userId | Long | Y | 팀원 ID |
| memberPermissions[].canNudge | Boolean | Y | 독촉 발송 권한 여부 |

**Response `200`**

```json
{
  "success": true,
  "message": "팀 설정이 저장되었습니다.",
  "data": null
}
```

**Error Cases**

| 상태 코드 | 메시지 |
|-----------|--------|
| 403 | 팀장만 설정을 변경할 수 있습니다. |

---

### 3-7. 팀 탈퇴

**DELETE** `/teams/{teamId}/members/me`

인증 필요 | 팀장은 다른 팀원에게 팀장 위임 후 탈퇴 가능

**Response `200`**

```json
{
  "success": true,
  "message": "팀에서 탈퇴했습니다.",
  "data": null
}
```

**Error Cases**

| 상태 코드 | 메시지 |
|-----------|--------|
| 400 | 팀장은 팀장을 위임한 후 탈퇴할 수 있습니다. |

---

## 4. 작업 API (`/teams/{teamId}/tasks`)

### 4-1. 작업 생성

**POST** `/teams/{teamId}/tasks`

인증 필요 | 팀 소속 모든 멤버 가능

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| teamId | Long | 팀 ID |

**Request Body**

```json
{
  "title": "UI 화면 구현",
  "description": "로그인 화면 및 메인 화면 레이아웃 작성",
  "assigneeId": 2,
  "dueDate": "2026-05-25"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | Y | 작업 제목 |
| description | String | N | 작업 상세 설명 (최대 1000자) |
| assigneeId | Long | N | 담당자 유저 ID (미지정 시 자기 자신) |
| dueDate | String | N | 마감일 (yyyy-MM-dd) |

**Response `201`**

```json
{
  "success": true,
  "message": "작업이 생성되었습니다.",
  "data": {
    "taskId": 10,
    "title": "UI 화면 구현",
    "description": "로그인 화면 및 메인 화면 레이아웃 작성",
    "status": "PENDING",
    "assignee": {
      "userId": 2,
      "username": "김철수",
      "color": "#34D399"
    },
    "createdBy": {
      "userId": 1,
      "username": "홍길동"
    },
    "dueDate": "2026-05-25",
    "startedAt": null,
    "completedAt": null,
    "createdAt": "2026-05-19T10:30:00"
  }
}
```

---

### 4-2. 팀 전체 작업 목록 조회

**GET** `/teams/{teamId}/tasks`

인증 필요 | 팀 소속 멤버만 가능

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| status | String | N | 상태 필터 (`PENDING`, `IN_PROGRESS`, `COMPLETED`) |
| assigneeId | Long | N | 담당자 필터 |

**Response `200`**

```json
{
  "success": true,
  "message": "작업 목록 조회 성공",
  "data": [
    {
      "taskId": 10,
      "title": "UI 화면 구현",
      "description": "로그인 화면 및 메인 화면 레이아웃 작성",
      "status": "IN_PROGRESS",
      "assignee": {
        "userId": 2,
        "username": "김철수",
        "color": "#34D399"
      },
      "dueDate": "2026-05-25",
      "dDay": -5,
      "nudgeCount": 3,
      "createdAt": "2026-05-19T10:30:00"
    },
    {
      "taskId": 11,
      "title": "API 명세서 작성",
      "description": null,
      "status": "COMPLETED",
      "assignee": {
        "userId": 1,
        "username": "홍길동",
        "color": "#A78BFA"
      },
      "dueDate": "2026-05-20",
      "dDay": 1,
      "nudgeCount": 0,
      "createdAt": "2026-05-18T09:00:00"
    }
  ]
}
```

> **dDay**: 양수=여유일, 음수=초과일, 0=당일

---

### 4-3. 내 작업 목록 조회

**GET** `/teams/{teamId}/tasks/my`

인증 필요 | 로그인된 사용자에게 할당된 작업만 반환

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| status | String | N | 상태 필터 (`PENDING`, `IN_PROGRESS`, `COMPLETED`) |

**Response `200`**

```json
{
  "success": true,
  "message": "내 작업 목록 조회 성공",
  "data": [
    {
      "taskId": 10,
      "title": "UI 화면 구현",
      "status": "IN_PROGRESS",
      "dueDate": "2026-05-25",
      "dDay": -5,
      "nudgeCount": 3,
      "createdAt": "2026-05-19T10:30:00"
    }
  ]
}
```

---

### 4-4. 작업 상태 변경

**PATCH** `/teams/{teamId}/tasks/{taskId}/status`

인증 필요 | 해당 작업의 담당자 또는 팀장만 가능

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| teamId | Long | 팀 ID |
| taskId | Long | 작업 ID |

**Request Body**

```json
{
  "status": "IN_PROGRESS"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| status | String | Y | `PENDING` / `IN_PROGRESS` / `COMPLETED` |

**Response `200`**

```json
{
  "success": true,
  "message": "작업 상태가 변경되었습니다.",
  "data": {
    "taskId": 10,
    "status": "IN_PROGRESS",
    "startedAt": "2026-05-19T14:00:00",
    "completedAt": null
  }
}
```

**Error Cases**

| 상태 코드 | 메시지 |
|-----------|--------|
| 403 | 담당자 또는 팀장만 상태를 변경할 수 있습니다. |

---

## 5. 독촉 알림 API (`/teams/{teamId}/tasks/{taskId}/nudge`)

### 5-1. 독촉 알림 발송

**POST** `/teams/{teamId}/tasks/{taskId}/nudge`

인증 필요 | 팀장 또는 독촉 권한이 있는 팀원만 가능

작업 담당자에게 FCM 푸시 알림을 즉시 발송하고 로그를 저장

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| teamId | Long | 팀 ID |
| taskId | Long | 독촉할 작업 ID |

**Request Body** 없음

**Response `200`**

```json
{
  "success": true,
  "message": "독촉 알림이 발송되었습니다.",
  "data": {
    "nudgeLogId": 55,
    "taskId": 10,
    "taskTitle": "UI 화면 구현",
    "sentTo": {
      "userId": 2,
      "username": "김철수"
    },
    "sentBy": {
      "userId": 1,
      "username": "홍길동"
    },
    "sentAt": "2026-05-19T14:30:00"
  }
}
```

**Error Cases**

| 상태 코드 | 메시지 |
|-----------|--------|
| 403 | 독촉 권한이 없습니다. |
| 400 | 이미 완료된 작업입니다. |
| 404 | 작업을 찾을 수 없습니다. |

---

### 5-2. 독촉 로그 조회

**GET** `/teams/{teamId}/nudge-logs`

인증 필요 | 팀 소속 멤버만 가능

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| taskId | Long | N | 특정 작업의 독촉 로그만 조회 |

**Response `200`**

```json
{
  "success": true,
  "message": "독촉 로그 조회 성공",
  "data": {
    "totalCount": 12,
    "logs": [
      {
        "nudgeLogId": 55,
        "taskId": 10,
        "taskTitle": "UI 화면 구현",
        "sentTo": {
          "userId": 2,
          "username": "김철수"
        },
        "sentBy": {
          "userId": 1,
          "username": "홍길동"
        },
        "sentAt": "2026-05-19T14:30:00"
      }
    ]
  }
}
```

---

## 6. API 요약표

| 기능 | 메서드 | 경로 | 인증 | 권한 |
|------|--------|------|------|------|
| 회원가입 | POST | `/auth/register` | X | - |
| 로그인 | POST | `/auth/login` | X | - |
| 토큰 갱신 | POST | `/auth/refresh` | X | - |
| 로그아웃 | POST | `/auth/logout` | O | - |
| 내 프로필 조회 | GET | `/users/me` | O | - |
| 내 프로필 수정 | PATCH | `/users/me` | O | - |
| FCM 토큰 등록 | PUT | `/users/me/fcm-token` | O | - |
| 팀 생성 | POST | `/teams` | O | - |
| 내 팀 목록 조회 | GET | `/teams` | O | - |
| 팀 상세 조회 | GET | `/teams/{teamId}` | O | 팀원 |
| 초대 링크 조회 | GET | `/teams/{teamId}/invite` | O | 팀원 |
| 초대 코드로 참가 | POST | `/teams/join` | O | - |
| 팀 설정 수정 | PATCH | `/teams/{teamId}/settings` | O | **팀장** |
| 팀 탈퇴 | DELETE | `/teams/{teamId}/members/me` | O | 팀원 |
| 작업 생성 | POST | `/teams/{teamId}/tasks` | O | 팀원 |
| 팀 전체 작업 조회 | GET | `/teams/{teamId}/tasks` | O | 팀원 |
| 내 작업 조회 | GET | `/teams/{teamId}/tasks/my` | O | 팀원 |
| 작업 상태 변경 | PATCH | `/teams/{teamId}/tasks/{taskId}/status` | O | 담당자/팀장 |
| 독촉 알림 발송 | POST | `/teams/{teamId}/tasks/{taskId}/nudge` | O | 팀장/독촉권한자 |
| 독촉 로그 조회 | GET | `/teams/{teamId}/nudge-logs` | O | 팀원 |

---

## 7. 주요 도메인 모델

### 작업 상태 (`TaskStatus`)

| 값 | 설명 |
|----|------|
| `PENDING` | 대기 |
| `IN_PROGRESS` | 진행 중 |
| `COMPLETED` | 완료 |

### 팀 역할 (`TeamRole`)

| 값 | 설명 |
|----|------|
| `LEADER` | 팀장 |
| `MEMBER` | 팀원 |

---

## 8. FCM 푸시 알림 페이로드

서버에서 FCM으로 전송하는 메시지 형식

```json
{
  "token": "담당자_FCM_토큰",
  "notification": {
    "title": "독촉 알림 🔔",
    "body": "홍길동님이 'UI 화면 구현' 작업을 독촉했습니다."
  },
  "data": {
    "type": "NUDGE",
    "teamId": "1",
    "taskId": "10",
    "taskTitle": "UI 화면 구현",
    "sentBy": "홍길동"
  }
}
```
