package com.nudgeup.app.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import com.nudgeup.app.data.local.TokenManager;
import com.nudgeup.app.data.repository.AuthRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<LoginViewModel.UiResult> registerResult = new MutableLiveData<>();

    public RegisterViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final TokenManager tokenManager;
        public Factory(TokenManager tokenManager) { this.tokenManager = tokenManager; }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new RegisterViewModel(new AuthRepository(tokenManager));
        }
    }

    public LiveData<LoginViewModel.UiResult> getRegisterResult() { return registerResult; }

    public void register(String userId, String username, String email, String password) {
        authRepository.register(userId, username, email, password).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    registerResult.setValue(LoginViewModel.UiResult.success());
                } else {
                    String msg = (response.body() != null) ? response.body().getMessage() : "회원가입에 실패했습니다.";
                    registerResult.setValue(LoginViewModel.UiResult.fail(msg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                registerResult.setValue(LoginViewModel.UiResult.fail("네트워크 오류가 발생했습니다."));
            }
        });
    }
}
