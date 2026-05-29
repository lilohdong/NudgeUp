package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.response.ApiResponse;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface NudgeApi {

    @POST("teams/{teamId}/tasks/{taskId}/nudge")
    Call<ApiResponse<Void>> sendNudge(
            @Path("teamId") long teamId,
            @Path("taskId") long taskId);
}
