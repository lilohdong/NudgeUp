package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.LoginRequest;
import com.nudgeup.app.data.dto.request.RefreshRequest;
import com.nudgeup.app.data.dto.request.RegisterRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.AuthResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/register")
    Call<ApiResponse<AuthResponse>> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<ApiResponse<AuthResponse>> refresh(@Body RefreshRequest request);
}
