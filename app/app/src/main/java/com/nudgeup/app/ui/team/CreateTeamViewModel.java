package com.nudgeup.app.ui.team;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.repository.TeamRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTeamViewModel extends ViewModel {

    private final TeamRepository teamRepository;

    public CreateTeamViewModel(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public static class Factory implements ViewModelProvider.Factory {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new CreateTeamViewModel(new TeamRepository());
        }
    }

    private final MutableLiveData<Boolean> createResult = new MutableLiveData<>();
    private final MutableLiveData<String> networkError = new MutableLiveData<>();

    public LiveData<Boolean> getCreateResult() { return createResult; }
    public LiveData<String> getNetworkError() { return networkError; }

    public void createTeam(String name) {
        teamRepository.createTeam(name).enqueue(new Callback<ApiResponse<TeamDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamDetailResponse>> call,
                                   Response<ApiResponse<TeamDetailResponse>> response) {
                createResult.setValue(response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess());
            }

            @Override
            public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {
                networkError.setValue("네트워크 오류가 발생했습니다.");
                createResult.setValue(false);
            }
        });
    }
}
