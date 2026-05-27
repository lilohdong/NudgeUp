package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.response.ApiResponse;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface NudgeApi {

    @POST("nudges/{taskId}")
    Call<ApiResponse<Void>> sendNudge(@Path("taskId") long taskId);
}
