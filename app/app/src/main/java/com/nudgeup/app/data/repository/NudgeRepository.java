package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.NudgeApi;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.global.network.ApiClient;
import retrofit2.Call;

public class NudgeRepository {

    private final NudgeApi nudgeApi;

    public NudgeRepository() {
        this.nudgeApi = ApiClient.getInstance().create(NudgeApi.class);
    }

    public Call<ApiResponse<Void>> sendNudge(long teamId, long taskId) {
        return nudgeApi.sendNudge(teamId, taskId);
    }
}
