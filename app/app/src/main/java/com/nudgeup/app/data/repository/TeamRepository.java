package com.nudgeup.app.data.repository;

import com.nudgeup.app.data.api.TeamApi;
import com.nudgeup.app.data.dto.request.CreateTeamRequest;
import com.nudgeup.app.data.dto.request.JoinTeamRequest;
import com.nudgeup.app.data.dto.request.TeamSettingsRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
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

    public Call<ApiResponse<TeamDetailResponse>> createTeam(String name, String color) {
        return teamApi.createTeam(new CreateTeamRequest(name, color));
    }

    public Call<ApiResponse<TeamDetailResponse>> getTeamDetail(long teamId) {
        return teamApi.getTeamDetail(teamId);
    }

    public Call<ApiResponse<Void>> updateSettings(long teamId, TeamSettingsRequest request) {
        return teamApi.updateSettings(teamId, request);
    }

    public Call<ApiResponse<TeamResponse>> joinTeam(String inviteCode) {
        return teamApi.joinTeam(new JoinTeamRequest(inviteCode));
    }

    public Call<ApiResponse<Void>> leaveTeam(long teamId) {
        return teamApi.leaveTeam(teamId);
    }

    public Call<ApiResponse<Void>> kickMember(long teamId, long memberId) {
        return teamApi.kickMember(teamId, memberId);
    }
}
