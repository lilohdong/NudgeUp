package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.TaskApi;
import com.nudgeup.app.data.dto.request.CreateTaskRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.global.network.ApiClient;
import java.util.List;
import retrofit2.Call;

public class TaskRepository {

    private final TaskApi taskApi;

    public TaskRepository() {
        this.taskApi = ApiClient.getInstance().create(TaskApi.class);
    }

    public Call<ApiResponse<List<TaskResponse>>> getTasks(long teamId) {
        return taskApi.getTasks(teamId);
    }

    public Call<ApiResponse<TaskResponse>> createTask(String title, String description, Long assigneeId, String dueDate) {
        return taskApi.createTask(new CreateTaskRequest(title, description, assigneeId, dueDate));
    }

    public Call<ApiResponse<TaskResponse>> updateStatus(long taskId, String status) {
        return taskApi.updateStatus(taskId, status);
    }
}
