package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.CreateTaskRequest;
import com.nudgeup.app.data.dto.request.UpdateStatusRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TaskApi {

    @GET("teams/{teamId}/tasks")
    Call<ApiResponse<List<TaskResponse>>> getTasks(@Path("teamId") long teamId);

    @GET("teams/{teamId}/tasks/my")
    Call<ApiResponse<List<TaskResponse>>> getMyTasks(@Path("teamId") long teamId);

    @POST("teams/{teamId}/tasks")
    Call<ApiResponse<TaskResponse>> createTask(
            @Path("teamId") long teamId,
            @Body CreateTaskRequest request);

    @PATCH("teams/{teamId}/tasks/{taskId}/status")
    Call<ApiResponse<TaskResponse>> updateStatus(
            @Path("teamId") long teamId,
            @Path("taskId") long taskId,
            @Body UpdateStatusRequest request);
}
