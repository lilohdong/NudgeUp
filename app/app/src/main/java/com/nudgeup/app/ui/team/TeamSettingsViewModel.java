package com.nudgeup.app.ui.team;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.data.dto.request.MemberPermissionRequest;
import com.nudgeup.app.data.dto.request.TeamSettingsRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.repository.TeamRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamSettingsViewModel extends ViewModel {

    private final TeamRepository teamRepository = new TeamRepository();

    private final MutableLiveData<List<MemberResponse>> members   = new MutableLiveData<>();
    private final MutableLiveData<Boolean>              saveResult  = new MutableLiveData<>();
    private final MutableLiveData<String>               inviteCode  = new MutableLiveData<>();

    private final Map<Long, Boolean> pendingNudgePermissions = new HashMap<>();
    private Long pendingNewLeaderId = null;

    public LiveData<List<MemberResponse>> getMembers()   { return members; }
    public LiveData<Boolean>             getSaveResult() { return saveResult; }
    public LiveData<String>              getInviteCode() { return inviteCode; }

    public void loadMembers(long teamId) {
        teamRepository.getTeamDetail(teamId).enqueue(new Callback<ApiResponse<TeamDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamDetailResponse>> call,
                                   Response<ApiResponse<TeamDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TeamDetailResponse detail = response.body().getData();
                    if (detail.getMembers() != null) {
                        members.setValue(detail.getMembers());
                    }
                    inviteCode.setValue(detail.getInviteCode());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {}
        });
    }

    public void setNudgePermission(long userId, boolean canNudge) {
        pendingNudgePermissions.put(userId, canNudge);
    }

    public void setNewLeader(long userId) {
        pendingNewLeaderId = userId;
    }

    public void saveSettings(long teamId) {
        List<MemberPermissionRequest> permissionList = new ArrayList<>();
        for (Map.Entry<Long, Boolean> entry : pendingNudgePermissions.entrySet()) {
            permissionList.add(new MemberPermissionRequest(entry.getKey(), entry.getValue()));
        }
        TeamSettingsRequest request = new TeamSettingsRequest(pendingNewLeaderId, permissionList);
        teamRepository.updateSettings(teamId, request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                saveResult.setValue(response.isSuccessful());
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                saveResult.setValue(false);
            }
        });
    }
}
