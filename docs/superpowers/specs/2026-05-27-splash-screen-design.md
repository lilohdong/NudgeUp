# Splash Screen Design — NudgeUp Android

**Date:** 2026-05-27  
**Approach:** SplashScreen API (androidx.core:core-splashscreen)

---

## Overview

앱 초기 실행 시 로고와 다크 배경을 보여주는 스플래시 화면을 구현한다.  
Android 12+에서는 OS 수준의 입장 애니메이션이 자동 제공되며, API 24~31에서는 compat 라이브러리가 동일하게 처리한다.

---

## Architecture

### 1. 의존성 추가

`app/app/build.gradle.kts`에 `androidx.core:core-splashscreen` 추가.

### 2. 스플래시 테마 정의

`res/values/themes.xml`에 `Theme.NudgeUp.Splash` 스타일 추가:

| 속성 | 값 | 설명 |
|------|----|------|
| `windowSplashScreenBackground` | `@color/bg_dark` (`#1A1A2E`) | 앱 다크 배경색 |
| `windowSplashScreenAnimatedIcon` | `@drawable/nudge_up_logo` | 기존 로고 파일 사용 |
| `postSplashScreenTheme` | `@style/Theme.NudgeUp` | 스플래시 이후 적용할 테마 |

`res/values-night/themes.xml`도 동일하게 업데이트.

### 3. AndroidManifest 수정

`LoginActivity`의 `android:theme`을 `@style/Theme.NudgeUp.Splash`로 변경.

### 4. LoginActivity 수정

`onCreate()` 최상단에서 `SplashScreen.installSplashScreen(this)` 호출.  
이후 `TokenManager.isLoggedIn()` 확인:
- `true` → `MainActivity`로 이동 후 `finish()`
- `false` → 기존 로그인 UI 표시

---

## Data Flow

```
앱 시작
  └─ OS: 스플래시 테마 배경 + 로고 표시 (Activity 로드 전)
       └─ LoginActivity.onCreate()
            ├─ SplashScreen.installSplashScreen(this)  ← 스플래시 제어권 획득
            ├─ TokenManager.isLoggedIn()
            │    ├─ true  → MainActivity 이동 + finish()
            │    └─ false → setContentView(로그인 UI)
            └─ 기존 로그인 흐름 유지
```

---

## What NOT to do in splash

- 네트워크 요청 금지 (토큰 갱신 포함)
- 데이터 프리로드 금지
- `Handler.postDelayed`로 인위적 대기 금지

`Application.onCreate()`에서 이미 `TokenManager`가 초기화되므로 별도 로딩 불필요.

---

## Files to Change

| 파일 | 변경 내용 |
|------|-----------|
| `app/app/build.gradle.kts` | `core-splashscreen` 의존성 추가 |
| `res/values/themes.xml` | `Theme.NudgeUp.Splash` 스타일 추가 |
| `res/values-night/themes.xml` | 동일 스타일 추가 |
| `AndroidManifest.xml` | LoginActivity theme 변경 |
| `ui/auth/LoginActivity.java` | `installSplashScreen` 호출 + 토큰 체크 라우팅 |

---

## Out of Scope

- 커스텀 애니메이션 (OnExitAnimationListener)
- 스플래시 중 비동기 데이터 로딩
- 다크/라이트 테마 전환 (앱 전체가 다크 고정)
