package com.nudgeup.app.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.AuthResponse;
import com.nudgeup.app.data.repository.AuthRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<LoginViewModel.UiResult> registerResult = new MutableLiveData<>();

    public RegisterViewModel() {
        authRepository = new AuthRepository(NudgeUpApplication.getTokenManager());
    }

    public LiveData<LoginViewModel.UiResult> getRegisterResult() { return registerResult; }

    public void register(String email, String password, String name) {
        authRepository.register(email, password, name).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse data = response.body().getData();
                    authRepository.saveTokens(data.getAccessToken(), data.getRefreshToken());
                    if (data.getUser() != null) {
                        authRepository.saveUserId(data.getUser().getId());
                    }
                    registerResult.setValue(LoginViewModel.UiResult.success());
                } else {
                    registerResult.setValue(LoginViewModel.UiResult.fail("회원가입에 실패했습니다."));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                registerResult.setValue(LoginViewModel.UiResult.fail("네트워크 오류가 발생했습니다."));
            }
        });
    }
}
