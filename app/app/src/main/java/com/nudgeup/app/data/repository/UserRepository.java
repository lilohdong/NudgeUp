package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.UserApi;
import com.nudgeup.app.data.dto.request.FcmTokenRequest;
import com.nudgeup.app.data.dto.request.UpdateColorRequest;
import com.nudgeup.app.data.dto.request.UpdateUsernameRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import com.nudgeup.app.global.network.ApiClient;
import retrofit2.Call;

public class UserRepository {

    private final UserApi userApi;

    public UserRepository() {
        this.userApi = ApiClient.getInstance().create(UserApi.class);
    }

    public Call<ApiResponse<UserResponse>> getMe() {
        return userApi.getMe();
    }

    public Call<ApiResponse<Void>> registerFcmToken(String token) {
        return userApi.registerFcmToken(new FcmTokenRequest(token));
    }

    public Call<ApiResponse<UserResponse>> updateUsername(String username) {
        return userApi.updateUsername(new UpdateUsernameRequest(username));
    }

    public Call<ApiResponse<UserResponse>> updateColor(String colorHex) {
        return userApi.updateColor(new UpdateColorRequest(colorHex));
    }

    public Call<ApiResponse<Void>> deleteMe() {
        return userApi.deleteMe();
    }
}
