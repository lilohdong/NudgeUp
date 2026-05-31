package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NudgeApi {

    @POST("teams/{teamId}/tasks/{taskId}/nudge")
    Call<ApiResponse<Void>> sendNudge(
            @Path("teamId") long teamId,
            @Path("taskId") long taskId);

    @GET("nudges/received")
    Call<ApiResponse<List<NudgeLogItemResponse>>> getReceivedNudgeLogs();

    @GET("nudges/received/count")
    Call<ApiResponse<Map<String, Long>>> getReceivedNudgeCount(
            @Query("days") int days);
}
