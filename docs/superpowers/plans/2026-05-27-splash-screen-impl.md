# Splash Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 앱 초기 실행 시 `nudge_up_logo` 로고와 다크 배경을 보여주는 스플래시 화면을 SplashScreen API로 구현하고, 로그인 상태에 따라 자동 라우팅한다.

**Architecture:** `androidx.core:core-splashscreen` 라이브러리를 사용해 OS 수준 스플래시를 적용한다. `LoginActivity`가 런처이므로, 해당 Activity에 스플래시 테마를 적용하고 `onCreate()` 최상단에서 `SplashScreen.installSplashScreen(this)`를 호출한다. 토큰이 존재하면 `MainActivity`로 즉시 라우팅하고, 없으면 기존 로그인 UI를 표시한다.

**Tech Stack:** Android Java 17, `androidx.core:core-splashscreen:1.0.1`, JUnit4 Espresso 인스트루먼트 테스트

---

## 파일 변경 목록

| 구분 | 파일 |
|------|------|
| 수정 | `app/gradle/libs.versions.toml` |
| 수정 | `app/app/build.gradle.kts` |
| 수정 | `app/app/src/main/res/values/themes.xml` |
| 수정 | `app/app/src/main/res/values-night/themes.xml` |
| 수정 | `app/app/src/main/AndroidManifest.xml` |
| 수정 | `app/app/src/main/java/com/nudgeup/app/ui/auth/LoginActivity.java` |
| 생성 | `app/app/src/androidTest/java/com/nudgeup/app/ui/auth/LoginActivitySplashTest.java` |

---

## Task 1: 작업 브랜치 생성

**Files:**
- (git 작업만, 파일 변경 없음)

- [ ] **Step 1: 현재 브랜치 확인**

```bash
git status
git branch
```

Expected: `main` 브랜치에 있고 working tree가 clean함

- [ ] **Step 2: 새 브랜치 생성 및 전환**

```bash
git checkout -b feat/splash-screen
```

Expected: `Switched to a new branch 'feat/splash-screen'`

---

## Task 2: SplashScreen 의존성 추가

**Files:**
- Modify: `app/gradle/libs.versions.toml`
- Modify: `app/app/build.gradle.kts`

- [ ] **Step 1: libs.versions.toml에 버전 추가**

`[versions]` 섹션 끝에 추가:
```toml
coreSplashscreen = "1.0.1"
```

`[libraries]` 섹션 끝에 추가:
```toml
core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }
```

완성된 파일 (`app/gradle/libs.versions.toml`) 전체:
```toml
[versions]
agp = "9.1.1"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
appcompat = "1.7.1"
material = "1.13.0"
activity = "1.13.0"
constraintlayout = "2.2.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
lifecycle = "2.9.0"
securityCrypto = "1.1.0-alpha06"
firebaseBom = "33.7.0"
coreSplashscreen = "1.0.1"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }
ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
activity = { group = "androidx.activity", name = "activity", version.ref = "activity" }
constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel", version.ref = "lifecycle" }
lifecycle-livedata = { group = "androidx.lifecycle", name = "lifecycle-livedata", version.ref = "lifecycle" }
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }
core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

- [ ] **Step 2: build.gradle.kts에 의존성 추가**

`app/app/build.gradle.kts`의 `dependencies` 블록 내 `implementation(libs.security.crypto)` 아래에 추가:
```kotlin
// Splash Screen
implementation(libs.core.splashscreen)
```

- [ ] **Step 3: Gradle 싱크 확인**

Android Studio에서 **Sync Now** 클릭 또는:
```bash
cd app && ./gradlew app:dependencies --configuration releaseRuntimeClasspath | grep splashscreen
```

Expected: `androidx.core:core-splashscreen:1.0.1` 출력 확인

---

## Task 3: 스플래시 테마 정의

**Files:**
- Modify: `app/app/src/main/res/values/themes.xml`
- Modify: `app/app/src/main/res/values-night/themes.xml`

- [ ] **Step 1: values/themes.xml에 스플래시 테마 추가**

`app/app/src/main/res/values/themes.xml` 전체를 아래로 교체:
```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base application theme. -->
    <style name="Base.Theme.NudgeUp" parent="Theme.Material3.DayNight.NoActionBar">
    </style>

    <style name="Theme.NudgeUp" parent="Base.Theme.NudgeUp" />

    <!-- Splash screen theme — LoginActivity에만 적용 -->
    <style name="Theme.NudgeUp.Splash" parent="Theme.SplashScreen">
        <!-- 스플래시 배경색: 앱 다크 배경 -->
        <item name="windowSplashScreenBackground">@color/bg_dark</item>
        <!-- 스플래시 아이콘: 기존 로고 -->
        <item name="windowSplashScreenAnimatedIcon">@drawable/nudge_up_logo</item>
        <!-- 아이콘 뒤 배경(API 31+에서 원형 영역): 배경과 동일하게 -->
        <item name="windowSplashScreenIconBackgroundColor">@color/bg_dark</item>
        <!-- 스플래시 이후 적용할 앱 테마 -->
        <item name="postSplashScreenTheme">@style/Theme.NudgeUp</item>
    </style>
</resources>
```

- [ ] **Step 2: values-night/themes.xml에 동일 테마 추가**

`app/app/src/main/res/values-night/themes.xml` 전체를 아래로 교체:
```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base application theme (dark). -->
    <style name="Base.Theme.NudgeUp" parent="Theme.Material3.DayNight.NoActionBar">
    </style>

    <!-- Splash screen theme (dark mode) — LoginActivity에만 적용 -->
    <style name="Theme.NudgeUp.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/bg_dark</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/nudge_up_logo</item>
        <item name="windowSplashScreenIconBackgroundColor">@color/bg_dark</item>
        <item name="postSplashScreenTheme">@style/Theme.NudgeUp</item>
    </style>
</resources>
```

---

## Task 4: AndroidManifest.xml — LoginActivity 테마 변경

**Files:**
- Modify: `app/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: LoginActivity에 스플래시 테마 적용**

`app/app/src/main/AndroidManifest.xml`에서 LoginActivity 선언을 아래로 교체:
```xml
<activity
    android:name=".ui.auth.LoginActivity"
    android:exported="true"
    android:theme="@style/Theme.NudgeUp.Splash">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

---

## Task 5: LoginActivity — 스플래시 설치 + 로그인 상태 라우팅

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/auth/LoginActivity.java`

- [ ] **Step 1: 테스트 먼저 작성 (TDD)**

`app/app/src/androidTest/java/com/nudgeup/app/ui/auth/LoginActivitySplashTest.java` 파일을 새로 생성:

```java
package com.nudgeup.app.ui.auth;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.nudgeup.app.R;
import com.nudgeup.app.data.local.TokenManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class LoginActivitySplashTest {

    @Test
    public void whenNotLoggedIn_loginButtonIsVisible() {
        // ActivityScenarioRule은 @Before보다 먼저 Activity를 실행하므로
        // 토큰 초기화 후 수동으로 Activity를 실행한다
        Context ctx = ApplicationProvider.getApplicationContext();
        TokenManager.getInstance(ctx).clearTokens();

        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            Espresso.onView(withId(R.id.btn_login))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void whenNotLoggedIn_emailInputIsVisible() {
        Context ctx = ApplicationProvider.getApplicationContext();
        TokenManager.getInstance(ctx).clearTokens();

        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            Espresso.onView(withId(R.id.et_email))
                    .check(matches(isDisplayed()));
        }
    }
}
```

- [ ] **Step 2: 테스트를 먼저 실행해 실패 확인**

Android Studio에서 `LoginActivitySplashTest` 우클릭 → **Run** (또는 에뮬레이터 연결 후):
```bash
cd app && ./gradlew connectedAndroidTest --tests "com.nudgeup.app.ui.auth.LoginActivitySplashTest"
```

Expected: 컴파일 에러 없이 테스트 실패 (아직 `installSplashScreen` 미호출로 테마 불일치 가능) 또는 PASS  
→ 테스트가 실행 가능한 상태인지 확인하는 것이 목적

- [ ] **Step 3: LoginActivity에 스플래시 설치 + 라우팅 코드 추가**

`app/app/src/main/java/com/nudgeup/app/ui/auth/LoginActivity.java`를 아래로 교체:

```java
package com.nudgeup.app.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.R;
import com.nudgeup.app.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private EditText etEmail, etPassword;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 스플래시 설치 — super.onCreate() 이전 호출 필수
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // 이미 로그인된 경우 메인으로 이동
        if (NudgeUpApplication.getTokenManager().isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnBack = findViewById(R.id.btn_back);
        Button btnRegister = findViewById(R.id.btn_register_outline);
        CheckBox cbSaveId = findViewById(R.id.cb_save_id);
        TextView tvFindPassword = findViewById(R.id.tv_find_password);

        // 저장된 아이디 불러오기
        SharedPreferences prefs = getSharedPreferences("nudgeup_prefs", MODE_PRIVATE);
        boolean isSaved = prefs.getBoolean("save_id", false);
        cbSaveId.setChecked(isSaved);
        if (isSaved) etEmail.setText(prefs.getString("saved_id", ""));

        // 뒤로가기 (앱 종료)
        btnBack.setOnClickListener(v -> finish());

        // 비밀번호 표시 토글
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // 아이디 저장 체크박스
        cbSaveId.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("save_id", isChecked).apply();
        });

        // 비밀번호 찾기
        tvFindPassword.setOnClickListener(v ->
                startActivity(new Intent(this, FindPasswordActivity.class)));

        // 로그인 버튼
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cbSaveId.isChecked()) {
                prefs.edit().putString("saved_id", email).apply();
            }
            viewModel.login(email, password);
        });

        // 회원가입 버튼
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // 로그인 결과 관찰
        viewModel.getLoginResult().observe(this, result -> {
            if (result.isSuccess()) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
        }
        etPassword.setSelection(etPassword.getText().length());
    }
}
```

- [ ] **Step 4: 테스트 재실행해 PASS 확인**

```bash
cd app && ./gradlew connectedAndroidTest --tests "com.nudgeup.app.ui.auth.LoginActivitySplashTest"
```

Expected: `BUILD SUCCESSFUL`, 2 tests passed

---

## Task 6: 최종 커밋

**Files:**
- 변경된 모든 파일

- [ ] **Step 1: 변경 파일 확인**

```bash
git diff --stat
```

Expected: 아래 7개 파일이 표시됨
```
app/gradle/libs.versions.toml
app/app/build.gradle.kts
app/app/src/main/res/values/themes.xml
app/app/src/main/res/values-night/themes.xml
app/app/src/main/AndroidManifest.xml
app/app/src/main/java/com/nudgeup/app/ui/auth/LoginActivity.java
app/app/src/androidTest/java/com/nudgeup/app/ui/auth/LoginActivitySplashTest.java
```

- [ ] **Step 2: 스테이징 및 커밋**

```bash
git add app/gradle/libs.versions.toml
git add app/app/build.gradle.kts
git add app/app/src/main/res/values/themes.xml
git add app/app/src/main/res/values-night/themes.xml
git add app/app/src/main/AndroidManifest.xml
git add app/app/src/main/java/com/nudgeup/app/ui/auth/LoginActivity.java
git add app/app/src/androidTest/java/com/nudgeup/app/ui/auth/LoginActivitySplashTest.java
git commit -m "feat: SplashScreen API 적용 — 로고 스플래시 + 자동 로그인 라우팅"
```

---

## 완료 후 수동 확인 사항

1. **에뮬레이터/실기기에서 앱 최초 실행** → 로고가 다크 배경에 보이다가 로그인 화면으로 전환되는지 확인
2. **로그인 후 앱 재실행** → 스플래시 후 로그인 화면 없이 바로 메인 화면으로 이동하는지 확인
3. **Android 12+ 기기** → 로고에 기본 입장 애니메이션이 적용되는지 확인

> **참고:** `nudge_up_logo.png`가 텍스트 포함 전체 로고라면 API 31+ 원형 영역에서 잘릴 수 있음. 이 경우 `windowSplashScreenAnimatedIcon`을 아이콘 전용 이미지로 교체하거나, 크기를 조정할 것.
