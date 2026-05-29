package com.nudgeup.app.data.api;

import com.nudgeup.app.data.dto.request.CreateTeamRequest;
import com.nudgeup.app.data.dto.request.TeamSettingsRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.dto.response.TeamResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TeamApi {

    @GET("teams")
    Call<ApiResponse<List<TeamResponse>>> getMyTeams();

    @POST("teams")
    Call<ApiResponse<TeamDetailResponse>> createTeam(@Body CreateTeamRequest request);

    @GET("teams/{teamId}")
    Call<ApiResponse<TeamDetailResponse>> getTeamDetail(@Path("teamId") long teamId);

    @PATCH("teams/{teamId}/settings")
    Call<ApiResponse<Void>> updateSettings(
            @Path("teamId") long teamId,
            @Body TeamSettingsRequest request);
}
