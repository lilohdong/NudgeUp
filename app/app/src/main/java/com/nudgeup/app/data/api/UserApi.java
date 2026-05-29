package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.FcmTokenRequest;
import com.nudgeup.app.data.dto.request.UpdateUserRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;

public interface UserApi {

    @GET("users/me")
    Call<ApiResponse<UserResponse>> getMe();

    @PUT("users/me/fcm-token")
    Call<ApiResponse<Void>> registerFcmToken(@Body FcmTokenRequest request);

    @PATCH("users/me")
    Call<ApiResponse<UserResponse>> updateProfile(@Body UpdateUserRequest request);
}
