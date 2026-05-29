package com.nudgeup.app.ui.task;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.repository.TaskRepository;
import com.nudgeup.app.data.repository.TeamRepository;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTaskViewModel extends ViewModel {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;

    public CreateTaskViewModel(TaskRepository taskRepository, TeamRepository teamRepository) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
    }

    public static class Factory implements ViewModelProvider.Factory {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new CreateTaskViewModel(new TaskRepository(), new TeamRepository());
        }
    }

    private final MutableLiveData<Boolean> createResult = new MutableLiveData<>();
    private final MutableLiveData<List<MemberResponse>> members = new MutableLiveData<>();
    private final MutableLiveData<String> networkError = new MutableLiveData<>();

    public LiveData<Boolean> getCreateResult() { return createResult; }
    public LiveData<List<MemberResponse>> getMembers() { return members; }
    public LiveData<String> getNetworkError() { return networkError; }

    public void loadTeamMembers(long teamId) {
        teamRepository.getTeamDetail(teamId).enqueue(new Callback<ApiResponse<TeamDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamDetailResponse>> call,
                                   Response<ApiResponse<TeamDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TeamDetailResponse detail = response.body().getData();
                    if (detail.getMembers() != null) {
                        members.setValue(detail.getMembers());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {
                networkError.setValue("팀원 정보를 불러오지 못했습니다.");
            }
        });
    }

    public void createTask(long teamId, String title, String description, Long assigneeId, String dueDate) {
        taskRepository.createTask(teamId, title, description, assigneeId, dueDate)
                .enqueue(new Callback<ApiResponse<TaskResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<TaskResponse>> call,
                                           Response<ApiResponse<TaskResponse>> response) {
                        createResult.setValue(
                                response.isSuccessful() && response.body() != null && response.body().isSuccess());
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<TaskResponse>> call, Throwable t) {
                        createResult.setValue(false);
                    }
                });
    }
}
