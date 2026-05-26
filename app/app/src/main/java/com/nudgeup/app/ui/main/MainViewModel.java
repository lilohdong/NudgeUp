package com.nudgeup.app.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.MemberResponse;
import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.data.dto.response.TeamResponse;
import com.nudgeup.app.data.repository.NudgeRepository;
import com.nudgeup.app.data.repository.TaskRepository;
import com.nudgeup.app.data.repository.TeamRepository;
import com.nudgeup.app.domain.User;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {

    private final TeamRepository teamRepository = new TeamRepository();
    private final TaskRepository taskRepository = new TaskRepository();
    private final NudgeRepository nudgeRepository = new NudgeRepository();

    private final MutableLiveData<List<TeamResponse>> teams = new MutableLiveData<>();
    private final MutableLiveData<List<TaskResponse>> teamTasks = new MutableLiveData<>();
    private final MutableLiveData<List<TaskResponse>> myTasks = new MutableLiveData<>();
    private final MutableLiveData<List<MemberResponse>> members = new MutableLiveData<>();
    private final MutableLiveData<TeamResponse> currentTeam = new MutableLiveData<>();
    private final MutableLiveData<Boolean> nudgeResult = new MutableLiveData<>();
    private final MutableLiveData<Integer> nudgeCount = new MutableLiveData<>(0);

    // 목업용: 현재 사용자 ID
    private static final long MOCK_MY_USER_ID = 1L;

    private List<MemberResponse> currentMembers;
    private List<TaskResponse> allTasksCache = new ArrayList<>();
    private long selectedMemberId = -1L; // -1 = 전체

    public LiveData<List<TeamResponse>> getTeams() { return teams; }
    public LiveData<List<TaskResponse>> getTeamTasks() { return teamTasks; }
    public LiveData<List<TaskResponse>> getMyTasks() { return myTasks; }
    public LiveData<List<MemberResponse>> getMembers() { return members; }
    public LiveData<TeamResponse> getCurrentTeam() { return currentTeam; }
    public LiveData<Boolean> getNudgeResult() { return nudgeResult; }
    public LiveData<Integer> getNudgeCount() { return nudgeCount; }

    public void loadTeams() {
        // TODO: 백엔드 연결 후 목업 제거
        List<TeamResponse> mockTeams = Arrays.asList(
                new TeamResponse(1, "개발팀", "DEV001"),
                new TeamResponse(2, "디자인팀", "DES002")
        );
        teams.setValue(mockTeams);
        selectTeam(1);
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
    }

    public void loadTasks(long teamId) {
        // TODO: 백엔드 연결 후 목업 제거
        User 이원호 = new User(1, "이원호");
        User 오동준 = new User(2, "오동준");
        User 김민준 = new User(3, "김민준");
        User 이서연 = new User(4, "이서연");

        allTasksCache = new ArrayList<>(Arrays.asList(
                new TaskResponse(1, "로그인 API 연동",       "IN_PROGRESS", "2026-05-27", 이원호),
                new TaskResponse(2, "FCM 푸시 알림 구현",    "IN_PROGRESS", "2026-06-02", 오동준),
                new TaskResponse(3, "JWT 토큰 리프레시",     "PENDING",     "2026-06-10", 김민준),
                new TaskResponse(4, "DB 스키마 설계",        "COMPLETED",   "2026-05-20", 이서연),
                new TaskResponse(5, "UI 디자인 검수",        "IN_PROGRESS", "2026-05-28", 이원호),
                new TaskResponse(6, "팀원 초대 링크 구현",   "PENDING",     "2026-06-07", 이서연)
        ));

        applyFilter();
    }

    private void loadMembers(long teamId) {
        // TODO: 백엔드 연결 후 목업 제거
        currentMembers = Arrays.asList(
                new MemberResponse(1, "이원호", "LEADER", true),
                new MemberResponse(2, "오동준", "MEMBER", true),
                new MemberResponse(3, "김민준", "MEMBER", false),
                new MemberResponse(4, "이서연", "MEMBER", true)
        );
        members.setValue(currentMembers);
    }

    public void filterByMember(long memberId) {
        this.selectedMemberId = memberId;
        applyFilter();
    }

    private void applyFilter() {
        List<TaskResponse> filtered;
        if (selectedMemberId == -1L) {
            filtered = new ArrayList<>(allTasksCache);
        } else {
            filtered = new ArrayList<>();
            for (TaskResponse t : allTasksCache) {
                if (t.getAssignee() != null && t.getAssignee().getId() == selectedMemberId) {
                    filtered.add(t);
                }
            }
        }
        teamTasks.setValue(sortTasks(filtered));

        // 내 할일: 항상 MOCK_MY_USER_ID 기준
        List<TaskResponse> mine = new ArrayList<>();
        for (TaskResponse t : allTasksCache) {
            if (t.getAssignee() != null && t.getAssignee().getId() == MOCK_MY_USER_ID) {
                mine.add(t);
            }
        }
        myTasks.setValue(sortTasks(mine));
    }

    private List<TaskResponse> sortTasks(List<TaskResponse> list) {
        List<TaskResponse> sorted = new ArrayList<>(list);
        Collections.sort(sorted, (a, b) -> Integer.compare(getPriority(a), getPriority(b)));
        return sorted;
    }

    public int getPriority(TaskResponse task) {
        if ("COMPLETED".equals(task.getStatus())) return 3;
        long dday = calcDday(task.getDueDate());
        if (dday <= 3) return 0; // 마감임박
        if ("IN_PROGRESS".equals(task.getStatus())) return 1;
        return 2; // PENDING
    }

    public long calcDday(String dueDateStr) {
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

    public void createTeam(String name) {
        teamRepository.createTeam(name).enqueue(new Callback<ApiResponse<TeamResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamResponse>> call, Response<ApiResponse<TeamResponse>> response) {
                if (response.isSuccessful()) loadTeams();
            }
            @Override
            public void onFailure(Call<ApiResponse<TeamResponse>> call, Throwable t) {}
        });
    }

    public void sendNudge(long taskId) {
        // TODO: 백엔드 연결 후 실제 API 호출로 교체
        Integer count = nudgeCount.getValue();
        nudgeCount.setValue(count == null ? 1 : count + 1);
        nudgeResult.setValue(true);
    }

    public void updateTaskStatus(long taskId, String newStatus) {
        // TODO: 백엔드 연결 후 API 호출로 교체
        for (TaskResponse t : allTasksCache) {
            if (t.getId() == taskId) {
                allTasksCache.set(allTasksCache.indexOf(t),
                        new TaskResponse(t.getId(), t.getTitle(), newStatus, t.getDueDate(), t.getAssignee()));
                break;
            }
        }
        applyFilter();
    }

    public boolean isCurrentUserLeader() {
        if (currentMembers == null) return false;
        long myId = NudgeUpApplication.getTokenManager().getUserId();
        if (myId == 0) myId = MOCK_MY_USER_ID; // 목업 모드: 이원호는 팀장
        for (MemberResponse m : currentMembers) {
            if (m.getUserId() == myId && "LEADER".equals(m.getRole())) return true;
        }
        return false;
    }

    public long getMockMyUserId() {
        return MOCK_MY_USER_ID;
    }
}
