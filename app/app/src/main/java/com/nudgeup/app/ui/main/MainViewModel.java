package com.nudgeup.app.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.data.dto.response.TeamDetailResponse;
import com.nudgeup.app.data.dto.response.TeamResponse;
import com.nudgeup.app.data.repository.NudgeRepository;
import com.nudgeup.app.data.repository.TaskRepository;
import com.nudgeup.app.data.repository.TeamRepository;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {

    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;
    private final NudgeRepository nudgeRepository;

    public MainViewModel(TeamRepository teamRepository, TaskRepository taskRepository, NudgeRepository nudgeRepository) {
        this.teamRepository = teamRepository;
        this.taskRepository = taskRepository;
        this.nudgeRepository = nudgeRepository;
    }

    public static class Factory implements ViewModelProvider.Factory {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new MainViewModel(new TeamRepository(), new TaskRepository(), new NudgeRepository());
        }
    }

    private final MutableLiveData<List<TeamResponse>> teams = new MutableLiveData<>();
    private final MutableLiveData<List<TaskResponse>> teamTasks = new MutableLiveData<>();
    private final MutableLiveData<List<TaskResponse>> myTasks = new MutableLiveData<>();
    private final MutableLiveData<List<MemberResponse>> members = new MutableLiveData<>();
    private final MutableLiveData<TeamResponse> currentTeam = new MutableLiveData<>();
    private final MutableLiveData<Boolean> nudgeResult = new MutableLiveData<>();
    private final MutableLiveData<Integer> nudgeCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> networkError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> joinResult = new MutableLiveData<>();
    public LiveData<Boolean> getJoinResult() { return joinResult; }
    public void resetJoinResult() { joinResult.setValue(null); }

    private List<TaskResponse> allTasksCache = new ArrayList<>();
    private long selectedMemberId = -1L;

    public enum SortMode { DEFAULT, BY_PERSON, BY_DEADLINE }

    private SortMode teamTasksSortMode = SortMode.DEFAULT;
    private SortMode myTasksSortMode = SortMode.DEFAULT;

    public LiveData<List<TeamResponse>> getTeams() { return teams; }
    public LiveData<List<TaskResponse>> getTeamTasks() { return teamTasks; }
    public LiveData<List<TaskResponse>> getMyTasks() { return myTasks; }
    public LiveData<List<MemberResponse>> getMembers() { return members; }
    public LiveData<TeamResponse> getCurrentTeam() { return currentTeam; }
    public LiveData<Boolean> getNudgeResult() { return nudgeResult; }
    public LiveData<Integer> getNudgeCount() { return nudgeCount; }
    public LiveData<String> getNetworkError() { return networkError; }

    public void setTeamTasksSortMode(SortMode mode) {
        if (teamTasksSortMode == mode) return;
        teamTasksSortMode = mode;
        applyFilter();
    }

    public void setMyTasksSortMode(SortMode mode) {
        if (myTasksSortMode == mode) return;
        myTasksSortMode = mode;
        TeamResponse team = currentTeam.getValue();
        if (team != null) loadMyTasks(team.getId());
    }

    public void loadTeams() {
        teamRepository.getMyTeams().enqueue(new Callback<ApiResponse<List<TeamResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TeamResponse>>> call,
                                   Response<ApiResponse<List<TeamResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TeamResponse> list = response.body().getData();
                    teams.setValue(list);
                    if (list != null && !list.isEmpty()) {
                        selectTeam(list.get(0).getId());
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<TeamResponse>>> call, Throwable t) {
                networkError.setValue("네트워크 오류가 발생했습니다.");
            }
        });
    }

    public void selectTeam(long teamId) {
        List<TeamResponse> teamList = teams.getValue();
        if (teamList != null) {
            for (TeamResponse t : teamList) {
                if (t.getId() == teamId) {
                    currentTeam.setValue(t);
                    break;
                }
            }
        }
        selectedMemberId = -1L;
        loadMembers(teamId);
        loadTasks(teamId);
        loadReceivedNudgeCount();
    }

    public void loadTasks(long teamId) {
        taskRepository.getTasks(teamId).enqueue(new Callback<ApiResponse<List<TaskResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TaskResponse>>> call,
                                   Response<ApiResponse<List<TaskResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TaskResponse> data = response.body().getData();
                    allTasksCache = data != null ? new ArrayList<>(data) : new ArrayList<>();
                    applyFilter();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<TaskResponse>>> call, Throwable t) {
                networkError.setValue("네트워크 오류가 발생했습니다.");
            }
        });

        loadMyTasks(teamId);
    }

    private void loadMyTasks(long teamId) {
        taskRepository.getMyTasks(teamId).enqueue(new Callback<ApiResponse<List<TaskResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TaskResponse>>> call,
                                   Response<ApiResponse<List<TaskResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TaskResponse> data = response.body().getData();
                    List<TaskResponse> filtered = new ArrayList<>();
                    if (data != null) {
                        for (TaskResponse t : data) {
                            if (!"CANCELLED".equals(t.getStatus())) filtered.add(t);
                        }
                    }
                    myTasks.setValue(sortTasksStatic(filtered, myTasksSortMode));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<TaskResponse>>> call, Throwable t) {
                networkError.setValue("네트워크 오류가 발생했습니다.");
            }
        });
    }

    private void loadMembers(long teamId) {
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
                networkError.setValue("네트워크 오류가 발생했습니다.");
            }
        });
    }

    public void filterByMember(long memberId) {
        this.selectedMemberId = memberId;
        applyFilter();
    }

    private void applyFilter() {
        List<TaskResponse> filtered = new ArrayList<>();
        for (TaskResponse t : allTasksCache) {
            if ("CANCELLED".equals(t.getStatus())) continue;
            if (selectedMemberId == -1L) {
                filtered.add(t);
            } else if (t.getAssignee() != null && t.getAssignee().getId() == selectedMemberId) {
                filtered.add(t);
            }
        }
        teamTasks.setValue(sortTasksStatic(filtered, teamTasksSortMode));
    }

    public void loadReceivedNudgeCount() {
        nudgeRepository.getReceivedNudgeCount(3).enqueue(
                new Callback<ApiResponse<java.util.Map<String, Long>>>() {
                    @Override
                    public void onResponse(
                            Call<ApiResponse<java.util.Map<String, Long>>> call,
                            Response<ApiResponse<java.util.Map<String, Long>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            java.util.Map<String, Long> data = response.body().getData();
                            Long count = data != null ? data.get("count") : 0L;
                            nudgeCount.setValue(count != null ? count.intValue() : 0);
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<java.util.Map<String, Long>>> call,
                                          Throwable t) {
                        // 카운트 갱신 실패 무시
                    }
                });
    }

    public void sendNudge(long taskId) {
        TeamResponse team = currentTeam.getValue();
        if (team == null) return;
        nudgeRepository.sendNudge(team.getId(), taskId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    nudgeResult.setValue(true);
                } else {
                    nudgeResult.setValue(false);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                nudgeResult.setValue(false);
            }
        });
    }

    public void cancelTask(long taskId) {
        // 낙관적 업데이트: 즉시 캐시에서 제거해 UI에 바로 반영
        allTasksCache.removeIf(t -> t.getId() == taskId);
        applyFilter();
        List<TaskResponse> currentMyTasks = myTasks.getValue();
        if (currentMyTasks != null) {
            List<TaskResponse> updated = new ArrayList<>(currentMyTasks);
            updated.removeIf(t -> t.getId() == taskId);
            myTasks.setValue(updated);
        }

        TeamResponse team = currentTeam.getValue();
        if (team == null) return;
        taskRepository.updateStatus(team.getId(), taskId, "CANCELLED").enqueue(new Callback<ApiResponse<TaskResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TaskResponse>> call, Response<ApiResponse<TaskResponse>> response) {
                if (!response.isSuccessful()) {
                    // 서버 실패 시 실제 상태로 복원
                    loadTasks(team.getId());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TaskResponse>> call, Throwable t) {
                loadTasks(team.getId());
            }
        });
    }

    public void updateTaskStatus(long taskId, String newStatus) {
        TeamResponse team = currentTeam.getValue();
        if (team == null) return;
        taskRepository.updateStatus(team.getId(), taskId, newStatus).enqueue(new Callback<ApiResponse<TaskResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TaskResponse>> call, Response<ApiResponse<TaskResponse>> response) {
                if (response.isSuccessful()) {
                    loadTasks(team.getId());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TaskResponse>> call, Throwable t) {
                networkError.setValue("네트워크 오류가 발생했습니다.");
            }
        });
    }

    public void createTeam(String name) {
        teamRepository.createTeam(name).enqueue(new Callback<ApiResponse<TeamDetailResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamDetailResponse>> call, Response<ApiResponse<TeamDetailResponse>> response) {
                if (response.isSuccessful()) loadTeams();
            }
            @Override
            public void onFailure(Call<ApiResponse<TeamDetailResponse>> call, Throwable t) {
                networkError.setValue("네트워크 오류가 발생했습니다.");
            }
        });
    }

    public boolean isCurrentUserLeader() {
        TeamResponse team = currentTeam.getValue();
        return team != null && "LEADER".equals(team.getMyRole());
    }

    static List<TaskResponse> sortTasksStatic(List<TaskResponse> list, SortMode mode) {
        List<TaskResponse> sorted = new ArrayList<>(list);
        switch (mode) {
            case BY_PERSON:
                sorted.sort((a, b) -> {
                    String nameA = (a.getAssignee() != null) ? a.getAssignee().getName() : null;
                    String nameB = (b.getAssignee() != null) ? b.getAssignee().getName() : null;
                    if (nameA == null && nameB == null) return 0;
                    if (nameA == null) return 1;
                    if (nameB == null) return -1;
                    return nameA.compareTo(nameB);
                });
                break;
            case BY_DEADLINE:
                sorted.sort((a, b) -> {
                    String dateA = a.getDueDate();
                    String dateB = b.getDueDate();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateA.compareTo(dateB);
                });
                break;
            default:
                sorted.sort(Comparator.comparingInt(MainViewModel::getPriorityStatic));
                break;
        }
        return sorted;
    }

    static int getPriorityStatic(TaskResponse task) {
        if ("COMPLETED".equals(task.getStatus())) return 3;
        long dday = calcDdayStatic(task.getDueDate());
        if (dday <= 3) return 0;
        if ("IN_PROGRESS".equals(task.getStatus())) return 1;
        return 2;
    }

    static long calcDdayStatic(String dueDateStr) {
        if (dueDateStr == null) return 999L;
        try {
            String[] parts = dueDateStr.split("-");
            if (parts.length != 3) return 999L;
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            Calendar due = Calendar.getInstance();
            due.set(year, month, day, 0, 0, 0);
            due.set(Calendar.MILLISECOND, 0);
            long diffMs = due.getTimeInMillis() - today.getTimeInMillis();
            return diffMs / (24L * 60 * 60 * 1000);
        } catch (Exception e) {
            return 999L;
        }
    }

    public long calcDday(String dueDateStr) { return calcDdayStatic(dueDateStr); }
    public int getPriority(TaskResponse task) { return getPriorityStatic(task); }

    public void joinTeam(String inviteCode) {
        teamRepository.joinTeam(inviteCode).enqueue(new Callback<ApiResponse<TeamResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamResponse>> call,
                                   Response<ApiResponse<TeamResponse>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    joinResult.setValue(true);
                    loadTeams();
                } else {
                    joinResult.setValue(false);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TeamResponse>> call, Throwable t) {
                networkError.setValue("네트워크 오류가 발생했습니다.");
            }
        });
    }
}
