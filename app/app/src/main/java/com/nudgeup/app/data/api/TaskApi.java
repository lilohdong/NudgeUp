package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.CreateTaskRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TaskApi {

    @GET("tasks")
    Call<ApiResponse<List<TaskResponse>>> getTasks(@Query("teamId") long teamId);

    @POST("tasks")
    Call<ApiResponse<TaskResponse>> createTask(@Body CreateTaskRequest request);

    @PATCH("tasks/{taskId}/status")
    Call<ApiResponse<TaskResponse>> updateStatus(@Path("taskId") long taskId, @Body String status);
}
