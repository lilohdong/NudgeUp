package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.FcmTokenRequest;
import com.nudgeup.app.data.dto.request.UpdateColorRequest;
import com.nudgeup.app.data.dto.request.UpdateUsernameRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.UserResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;

public interface UserApi {

    @GET("users/me")
    Call<ApiResponse<UserResponse>> getMe();

    @PUT("users/me/fcm-token")
    Call<ApiResponse<Void>> registerFcmToken(@Body FcmTokenRequest request);

    @PATCH("users/me")
    Call<ApiResponse<UserResponse>> updateUsername(@Body UpdateUsernameRequest request);

    @PATCH("users/me/color")
    Call<ApiResponse<UserResponse>> updateColor(@Body UpdateColorRequest request);

    @DELETE("users/me")
    Call<ApiResponse<Void>> deleteMe();
}
