package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.AuthApi;
import com.nudgeup.app.data.dto.request.LoginRequest;
import com.nudgeup.app.data.dto.request.RegisterRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.AuthResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import com.nudgeup.app.data.local.TokenManager;
import com.nudgeup.app.global.network.ApiClient;
import retrofit2.Call;

public class AuthRepository {

    private final AuthApi authApi;
    private final TokenManager tokenManager;

    public AuthRepository(TokenManager tokenManager) {
        this.authApi = ApiClient.getInstance().create(AuthApi.class);
        this.tokenManager = tokenManager;
    }

    public Call<ApiResponse<AuthResponse>> login(String email, String password) {
        return authApi.login(new LoginRequest(email, password));
    }

    public Call<ApiResponse<UserResponse>> register(String email, String password, String username) {
        return authApi.register(new RegisterRequest(email, password, username));
    }

    public Call<ApiResponse<Void>> logout() {
        return authApi.logout();
    }

    public void saveTokens(String accessToken, String refreshToken) {
        tokenManager.saveTokens(accessToken, refreshToken);
    }

    public void saveUserId(long userId) {
        tokenManager.saveUserId(userId);
    }

    public void clearTokens() {
        tokenManager.clearTokens();
    }
}
