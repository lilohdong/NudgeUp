package com.nudgeup.app.ui.team;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.data.dto.request.TeamSettingsRequest;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.repository.TeamRepository;
import java.util.Arrays;
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
        // TODO: 백엔드 연결 후 목업 제거
        List<MemberResponse> mockMembers = Arrays.asList(
                new MemberResponse(1, "이원호", "LEADER", true),
                new MemberResponse(2, "오동준", "MEMBER", true),
                new MemberResponse(3, "김민준", "MEMBER", false),
                new MemberResponse(4, "이서연", "MEMBER", true)
        );
        members.setValue(mockMembers);
        // TODO: 백엔드 연결 후 팀 초대 코드 API에서 로드
        inviteCode.setValue("DEV001");

        /*
        teamRepository.getMembers(teamId).enqueue(new Callback<ApiResponse<List<MemberResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MemberResponse>>> call, Response<ApiResponse<List<MemberResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    members.setValue(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<MemberResponse>>> call, Throwable t) {}
        });
        */
    }

    public void setNudgePermission(long userId, boolean canNudge) {
        pendingNudgePermissions.put(userId, canNudge);
    }

    public void setNewLeader(long userId) {
        pendingNewLeaderId = userId;
    }

    public void saveSettings(long teamId) {
        // TODO: 백엔드 연결 후 API 호출로 교체
        saveResult.setValue(true);

        /*
        TeamSettingsRequest request = new TeamSettingsRequest(pendingNewLeaderId, pendingNudgePermissions);
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
        */
    }
}
