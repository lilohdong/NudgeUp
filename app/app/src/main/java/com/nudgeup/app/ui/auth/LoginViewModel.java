package com.nudgeup.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.AuthResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import com.nudgeup.app.data.local.TokenManager;
import com.nudgeup.app.data.repository.AuthRepository;
import com.nudgeup.app.data.repository.UserRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<UiResult> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoggedIn = new MutableLiveData<>();

    public LoginViewModel(AuthRepository authRepository, UserRepository userRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final TokenManager tokenManager;
        public Factory(TokenManager tokenManager) { this.tokenManager = tokenManager; }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new LoginViewModel(new AuthRepository(tokenManager), new UserRepository());
        }
    }

    public LiveData<UiResult> getLoginResult() { return loginResult; }
    public LiveData<Boolean> getIsLoggedIn() { return isLoggedIn; }

    /** 저장된 토큰으로 서버에 현재 사용자 정보를 조회해 로그인 상태를 검증한다. */
    public void checkLoginStatus() {
        userRepository.getMe().enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserResponse>> call,
                                   @NonNull Response<ApiResponse<UserResponse>> response) {
                isLoggedIn.setValue(response.isSuccessful());
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserResponse>> call, @NonNull Throwable t) {
                // 네트워크 오류 시 낙관적으로 로그인 허용 (오프라인 진입)
                isLoggedIn.setValue(true);
            }
        });
    }

    public void login(String userId, String password) {
        authRepository.login(userId, password).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthResponse>> call,
                                   @NonNull Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse data = response.body().getData();
                    authRepository.saveTokens(data.getAccessToken(), data.getRefreshToken());
                    if (data.getUser() != null) {
                        authRepository.saveUserId(data.getUser().getId());
                        authRepository.saveUsername(data.getUser().getName());
                    }
                    loginResult.setValue(UiResult.success());
                } else {
                    loginResult.setValue(UiResult.fail("아이디 또는 비밀번호를 확인해주세요."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Throwable t) {
                loginResult.setValue(UiResult.fail("네트워크 오류가 발생했습니다."));
            }
        });
    }

    public static class UiResult {
        private final boolean success;
        private final String message;

        private UiResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static UiResult success() { return new UiResult(true, null); }
        public static UiResult fail(String msg) { return new UiResult(false, msg); }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
