package com.nudgeup.app.ui.team;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
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

    private final TeamRepository teamRepository;

    public TeamSettingsViewModel(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public static class Factory implements ViewModelProvider.Factory {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new TeamSettingsViewModel(new TeamRepository());
        }
    }

    private final MutableLiveData<List<MemberResponse>> members      = new MutableLiveData<>();
    private final MutableLiveData<Boolean>              saveResult   = new MutableLiveData<>();
    private final MutableLiveData<String>               inviteCode   = new MutableLiveData<>();
    private final MutableLiveData<String>               networkError = new MutableLiveData<>();
    private final MutableLiveData<Boolean>              leaveResult  = new MutableLiveData<>();
    private final MutableLiveData<String>               leaveError   = new MutableLiveData<>();

    private final Map<Long, Boolean> pendingNudgePermissions = new HashMap<>();
    private Long pendingNewLeaderId = null;

    public LiveData<List<MemberResponse>> getMembers()      { return members; }
    public LiveData<Boolean>             getSaveResult()   { return saveResult; }
    public LiveData<String>              getInviteCode()   { return inviteCode; }
    public LiveData<String>              getNetworkError() { return networkError; }
    public LiveData<Boolean>             getLeaveResult()  { return leaveResult; }
    public LiveData<String>              getLeaveError()   { return leaveError; }

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
            public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {
                networkError.setValue("팀원 정보를 불러오지 못했습니다.");
            }
        });
    }

    public void setNudgePermission(long userId, boolean canNudge) {
        pendingNudgePermissions.put(userId, canNudge);
    }

    public void setNewLeader(long userId) {
        pendingNewLeaderId = userId;
    }

    public void leaveTeam(long teamId) {
        teamRepository.leaveTeam(teamId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    leaveResult.setValue(true);
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String body = response.errorBody().string();
                            org.json.JSONObject json = new org.json.JSONObject(body);
                            leaveError.setValue(json.optString("message", "탈퇴에 실패했습니다."));
                        }
                    } catch (Exception e) {
                        leaveError.setValue("탈퇴에 실패했습니다.");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                leaveError.setValue("네트워크 오류가 발생했습니다.");
            }
        });
    }

    public void kickMember(long teamId, long memberId) {
        teamRepository.kickMember(teamId, memberId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    loadMembers(teamId);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                networkError.setValue("네트워크 오류가 발생했습니다.");
            }
        });
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
