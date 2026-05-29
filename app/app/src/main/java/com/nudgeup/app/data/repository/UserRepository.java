package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.UserApi;
import com.nudgeup.app.data.dto.request.FcmTokenRequest;
import com.nudgeup.app.data.dto.request.UpdateUserRequest;
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

    public Call<ApiResponse<UserResponse>> updateProfile(String username, String color) {
        return userApi.updateProfile(new UpdateUserRequest(username, color));
    }
}
