package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.TeamApi;
import com.nudgeup.app.data.dto.request.CreateTeamRequest;
import com.nudgeup.app.data.dto.request.TeamSettingsRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.dto.response.TeamResponse;
import com.nudgeup.app.global.network.ApiClient;
import java.util.List;
import retrofit2.Call;

public class TeamRepository {

    private final TeamApi teamApi;

    public TeamRepository() {
        this.teamApi = ApiClient.getInstance().create(TeamApi.class);
    }

    public Call<ApiResponse<List<TeamResponse>>> getMyTeams() {
        return teamApi.getMyTeams();
    }

    public Call<ApiResponse<TeamResponse>> createTeam(String name) {
        return teamApi.createTeam(new CreateTeamRequest(name));
    }

    public Call<ApiResponse<List<MemberResponse>>> getMembers(long teamId) {
        return teamApi.getMembers(teamId);
    }

    public Call<ApiResponse<Void>> updateSettings(long teamId, TeamSettingsRequest request) {
        return teamApi.updateSettings(teamId, request);
    }
}
