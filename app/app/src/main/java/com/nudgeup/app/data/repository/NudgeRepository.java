package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.NudgeApi;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import com.nudgeup.app.global.network.ApiClient;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class NudgeRepository {

    private final NudgeApi nudgeApi;

    public NudgeRepository() {
        this.nudgeApi = ApiClient.getInstance().create(NudgeApi.class);
    }

    public Call<ApiResponse<Void>> sendNudge(long teamId, long taskId) {
        return nudgeApi.sendNudge(teamId, taskId);
    }

    public Call<ApiResponse<List<NudgeLogItemResponse>>> getReceivedNudgeLogs() {
        return nudgeApi.getReceivedNudgeLogs();
    }

    public Call<ApiResponse<Map<String, Long>>> getReceivedNudgeCount(int days) {
        return nudgeApi.getReceivedNudgeCount(days);
    }
}
