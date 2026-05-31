# fix/auth-token-validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 앱 시작 시 저장된 JWT 토큰을 서버에서 검증하고, 만료/비유효 토큰이면 로그인 화면으로 강제 이동한다.

**Architecture:** `LoginActivity`에서 토큰 존재 확인 후 `/users/me` API로 서버 검증 추가. `AuthInterceptor`에서 모든 401 응답을 가로채 토큰 삭제 + LoginActivity 재시작.

**Tech Stack:** Android Java, Retrofit2, OkHttp Interceptor

---

## File Map

| 파일 | 변경 유형 |
|---|---|
| `app/app/src/main/java/com/nudgeup/app/ui/auth/LoginActivity.java` | Modify |
| `app/app/src/main/java/com/nudgeup/app/global/network/AuthInterceptor.java` | Modify |

---

## Task 1: 브랜치 생성

- [ ] **Step 1: 브랜치 생성**

```bash
git checkout main && git pull origin main
git checkout -b fix/auth-token-validation
```

---

## Task 2: AuthInterceptor — 401 전역 처리

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/global/network/AuthInterceptor.java`

현재 `AuthInterceptor`는 요청에 토큰을 붙이기만 하고 응답을 확인하지 않는다.
401 응답 시 토큰 삭제 + `LoginActivity` 재시작 로직을 추가한다.

- [ ] **Step 1: AuthInterceptor 전체 교체**

```java
package com.nudgeup.app.global.network;

import android.content.Intent;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.ui.auth.LoginActivity;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = NudgeUpApplication.getTokenManager().getAccessToken();
        Request request = chain.request();
        if (token != null) {
            request = request.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
        }
        Response response = chain.proceed(request);
        if (response.code() == 401) {
            NudgeUpApplication.getTokenManager().clearTokens();
            Intent intent = new Intent(NudgeUpApplication.getInstance(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            NudgeUpApplication.getInstance().startActivity(intent);
        }
        return response;
    }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
cd B:/NudgeUp/app
./gradlew assembleDebug 2>&1 | tail -3
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```bash
git add app/app/src/main/java/com/nudgeup/app/global/network/AuthInterceptor.java
git commit -m "fix: 401 응답 시 토큰 삭제 후 로그인 화면으로 강제 이동"
```

---

## Task 3: LoginActivity — 토큰 서버 검증 후 이동

**Files:**
- Modify: `app/app/src/main/java/com/nudgeup/app/ui/auth/LoginActivity.java`

현재 코드:
```java
if (NudgeUpApplication.getTokenManager().isLoggedIn()) {
    startActivity(new Intent(this, MainActivity.class));
    finish();
    return;
}
```

서버 검증 없이 토큰 존재만으로 MainActivity로 이동한다. 토큰이 유효한지 `/users/me`로 확인해야 한다.

- [ ] **Step 1: LoginActivity onCreate 상단 블록 교체**

기존 4줄 블록을 아래로 교체한다:

```java
if (NudgeUpApplication.getTokenManager().isLoggedIn()) {
    // 서버에서 토큰 유효성 검증
    new com.nudgeup.app.data.repository.UserRepository().getMe()
            .enqueue(new retrofit2.Callback<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>>() {
                @Override
                public void onResponse(
                        retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> call,
                        retrofit2.Response<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> response) {
                    if (response.isSuccessful()) {
                        // 토큰 유효 → 메인으로
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        // 토큰 만료/비유효 → 토큰 삭제 후 로그인 화면 표시
                        NudgeUpApplication.getTokenManager().clearTokens();
                        setContentView(R.layout.activity_login);
                        setupLoginUi();
                    }
                }
                @Override
                public void onFailure(
                        retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> call,
                        Throwable t) {
                    // 네트워크 오류 → 오프라인 허용, 메인으로 이동
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            });
    return;
}
setContentView(R.layout.activity_login);
setupLoginUi();
```

- [ ] **Step 2: 기존 onCreate 본문을 setupLoginUi() 메서드로 추출**

`onCreate`의 `setContentView(R.layout.activity_login)` 이후 코드를 전부 새 `private void setupLoginUi()` 메서드로 이동한다. 기존 코드를 아래처럼 재구성:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (NudgeUpApplication.getTokenManager().isLoggedIn()) {
        new com.nudgeup.app.data.repository.UserRepository().getMe()
                .enqueue(new retrofit2.Callback<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> call,
                            retrofit2.Response<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> response) {
                        if (response.isSuccessful()) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            NudgeUpApplication.getTokenManager().clearTokens();
                            setContentView(R.layout.activity_login);
                            setupLoginUi();
                        }
                    }
                    @Override
                    public void onFailure(
                            retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> call,
                            Throwable t) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                });
        return;
    }

    setContentView(R.layout.activity_login);
    setupLoginUi();
}

private void setupLoginUi() {
    viewModel = new ViewModelProvider(this, new LoginViewModel.Factory(NudgeUpApplication.getTokenManager()))
            .get(LoginViewModel.class);

    etUserId = findViewById(R.id.et_email);
    etPassword = findViewById(R.id.et_password);
    btnTogglePassword = findViewById(R.id.btn_toggle_password);
    Button btnLogin = findViewById(R.id.btn_login);
    Button btnBack = findViewById(R.id.btn_back);
    Button btnRegister = findViewById(R.id.btn_register_outline);
    CheckBox cbSaveId = findViewById(R.id.cb_save_id);
    TextView tvFindPassword = findViewById(R.id.tv_find_password);

    SharedPreferences prefs = getSharedPreferences("nudgeup_prefs", MODE_PRIVATE);
    boolean isSaved = prefs.getBoolean("save_id", false);
    cbSaveId.setChecked(isSaved);
    if (isSaved) etUserId.setText(prefs.getString("saved_id", ""));

    btnBack.setOnClickListener(v -> finish());
    btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
    cbSaveId.setOnCheckedChangeListener((btn, isChecked) ->
            prefs.edit().putBoolean("save_id", isChecked).apply());
    tvFindPassword.setOnClickListener(v ->
            startActivity(new Intent(this, FindPasswordActivity.class)));

    btnLogin.setOnClickListener(v -> {
        String userId = etUserId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (userId.isEmpty() || password.isEmpty()) {
            ToastUtils.show(this, "아이디와 비밀번호를 입력해주세요.");
            return;
        }
        if (cbSaveId.isChecked()) {
            prefs.edit().putString("saved_id", userId).apply();
        }
        viewModel.login(userId, password);
    });

    btnRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

    viewModel.getLoginResult().observe(this, result -> {
        if (result.isSuccess()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            ToastUtils.show(this, result.getMessage());
        }
    });
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
git add app/app/src/main/java/com/nudgeup/app/ui/auth/LoginActivity.java
git commit -m "fix: 앱 시작 시 JWT 토큰 서버 검증 추가 (만료 토큰 감지)"
```

---

## Task 4: PR 생성

- [ ] **Step 1: push 및 PR**

```bash
git push -u origin fix/auth-token-validation
gh pr create --base main \
  --title "fix: JWT 토큰 서버 검증 및 401 전역 처리" \
  --body "## Summary
- LoginActivity: 저장된 토큰을 /users/me로 서버 검증 후 메인 이동 (만료/비유효 토큰 → 로그인 화면)
- AuthInterceptor: 모든 API 401 응답 시 토큰 삭제 + LoginActivity 재시작

## Test plan
- [ ] 정상 토큰: 앱 시작 → 로그인 화면 스킵, 메인 화면 표시
- [ ] 만료 토큰: 앱 시작 → 로그인 화면 표시
- [ ] 다른 계정 토큰: 앱 시작 → 서버에서 거부 시 로그인 화면
- [ ] 토큰 있을 때 서버 오류: 메인으로 이동 (오프라인 허용)"
```
