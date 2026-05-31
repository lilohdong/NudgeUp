package com.nudgeup.app.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.AuthResponse;
import com.nudgeup.app.data.local.TokenManager;
import com.nudgeup.app.data.repository.AuthRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<UiResult> loginResult = new MutableLiveData<>();

    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final TokenManager tokenManager;
        public Factory(TokenManager tokenManager) { this.tokenManager = tokenManager; }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new LoginViewModel(new AuthRepository(tokenManager));
        }
    }

    public LiveData<UiResult> getLoginResult() { return loginResult; }

    public void login(String userId, String password) {
        authRepository.login(userId, password).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
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
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
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
